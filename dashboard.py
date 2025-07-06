import streamlit as st
import pandas as pd
import pydeck as pdk
import os
from datetime import datetime
from geopy.distance import geodesic
from geopy.geocoders import Nominatim
import plotly.express as px

# === CONFIGURATION ===
st.set_page_config(page_title="VéliTracker", layout="wide")
st.markdown("""
    <style>
    html, body, [class*="css"] {
        font-family: 'Segoe UI', sans-serif;
        background-color: #0E1117;
        color: white;
    }
    .main-title {
        font-size: 2.5rem;
        font-weight: bold;
        text-align: center;
        color: #ffffff;
        text-shadow: 1px 1px 4px #118a7e;
        margin-bottom: 0.2rem;
    }
    .subtitle {
        text-align: center;
        font-size: 0.95rem;
        color: #aaa;
        margin-bottom: 1rem;
    }
    .kpi-block {
        padding: 0.8rem;
        border-radius: 10px;
        background-color: #111;
        box-shadow: 2px 2px 10px #00000070;
        text-align: center;
        margin-bottom: 1rem;
    }
    .kpi-title {
        font-size: 0.9rem;
        color: #ccc;
    }
    .kpi-value {
        font-size: 1.6rem;
        font-weight: bold;
        color: #fff;
    }
    .section-header {
        font-size: 1.2rem;
        margin-top: 2rem;
        font-weight: bold;
        color: white;
        background-color: #222;
        padding: 0.5rem;
        border-radius: 6px;
    }
    </style>
""", unsafe_allow_html=True)

# === DONNÉES ===
DATA_DIR = "velib_output"
json_files = [f for f in os.listdir(DATA_DIR) if f.endswith(".json")]
if not json_files:
    st.warning("Aucun fichier JSON trouvé.")
    st.stop()

latest_file = max([os.path.join(DATA_DIR, f) for f in json_files], key=os.path.getctime)
df = pd.read_json(latest_file, lines=True)
df["arrondissement"] = df["arrondissement"].fillna("Inconnu")

# === TITRE ===
st.markdown("<div class='main-title'>VéliTracker</div>", unsafe_allow_html=True)
st.markdown("<div class='subtitle'>Localisez, analysez et trouvez votre station idéale — "
            f"{datetime.now().strftime('%d/%m/%Y %H:%M:%S')}</div>", unsafe_allow_html=True)

# === RECHERCHE ADRESSE ===
st.markdown("<div class='section-header'>Recherche personnalisée</div>", unsafe_allow_html=True)
address = st.text_input("Entrez votre adresse ou ville (ex : 11 rue Réaumur, Paris)", "")
search = st.button("Lancer la recherche")

user_lat, user_lon = None, None
closest_station = None

if address and search:
    geolocator = Nominatim(user_agent="velib_app")
    location = geolocator.geocode(address)
    if location:
        user_lat, user_lon = location.latitude, location.longitude
        df["distance_m"] = df.apply(lambda row: geodesic((user_lat, user_lon), (row["lat"], row["lon"])).meters, axis=1)
        closest_station = df.loc[df["distance_m"].idxmin()]
        st.success(f"Station la plus proche : {closest_station['name']} à {int(closest_station['distance_m'])} m")
        maps_url = f"https://www.google.com/maps/dir/{user_lat},{user_lon}/{closest_station['lat']},{closest_station['lon']}"
        st.markdown(f"<a href='{maps_url}' target='_blank'>Voir l'itinéraire dans Google Maps</a>", unsafe_allow_html=True)

# === CARTE ===
layers = [pdk.Layer("ScatterplotLayer", data=df,
                    get_position='[lon, lat]', get_radius=100,
                    get_fill_color='[0, 255, 127, 160]', pickable=True)]

if closest_station is not None:
    user_df = pd.DataFrame([{"lat": user_lat, "lon": user_lon}])
    closest_df = pd.DataFrame([{
        "lat": closest_station["lat"],
        "lon": closest_station["lon"],
        "name": closest_station["name"],
        "ebike": closest_station["ebike"],
        "mechanical": closest_station["mechanical"],
        "numdocksavailable": closest_station["numdocksavailable"]
    }])
    line_df = pd.DataFrame({"coordinates": [[user_lon, user_lat], [closest_station["lon"], closest_station["lat"]]]})
    layers += [
        pdk.Layer("ScatterplotLayer", data=closest_df,
                  get_position='[lon, lat]', get_radius=140,
                  get_fill_color='[255, 0, 0, 180]', pickable=True),
        pdk.Layer("ScatterplotLayer", data=user_df,
                  get_position='[lon, lat]', get_radius=120,
                  get_fill_color='[0, 100, 255, 160]', pickable=False),
        pdk.Layer("PathLayer", data=line_df,
                  get_path="coordinates", get_width=3,
                  get_color=[255, 0, 0], width_min_pixels=2),
    ]

st.pydeck_chart(pdk.Deck(
    initial_view_state=pdk.ViewState(latitude=48.8566, longitude=2.3522, zoom=12),
    layers=layers,
    height=300,
))

# === KPIs ===
k1, k2, k3, k4 = st.columns(4)
with k1:
    st.markdown(f"<div class='kpi-block'><div class='kpi-title'>Vélos électriques</div><div class='kpi-value'>{int(df['ebike'].sum())}</div></div>", unsafe_allow_html=True)
with k2:
    st.markdown(f"<div class='kpi-block'><div class='kpi-title'>Vélos mécaniques</div><div class='kpi-value'>{int(df['mechanical'].sum())}</div></div>", unsafe_allow_html=True)
with k3:
    st.markdown(f"<div class='kpi-block'><div class='kpi-title'>Bornes vides</div><div class='kpi-value'>{int(df['numdocksavailable'].sum())}</div></div>", unsafe_allow_html=True)
with k4:
    st.markdown(f"<div class='kpi-block'><div class='kpi-title'>Vélos disponibles</div><div class='kpi-value'>{int(df['numbikesavailable'].sum())}</div></div>", unsafe_allow_html=True)

# === DONUT — PAR TYPE DE VÉLO ===
st.markdown("<div class='section-header'>Répartition filtrée des vélos disponibles</div>", unsafe_allow_html=True)
arr_options = sorted(df["arrondissement"].unique())
arr_selected = st.multiselect("Filtrer par arrondissement", arr_options, default=arr_options)
df_filtered = df[df["arrondissement"].isin(arr_selected)]
df_total = df_filtered.groupby("arrondissement")[["ebike", "mechanical"]].sum().reset_index()
df_total = df_total.melt(id_vars="arrondissement", value_vars=["ebike", "mechanical"],
                         var_name="type", value_name="total")

fig = px.pie(df_total, names='type', values='total', hole=0.4,
             color_discrete_map={"ebike": "#2ECC71", "mechanical": "#3498DB"})
st.plotly_chart(fig, use_container_width=True)

# === STATIONS PLEINES ===
st.markdown("<div class='section-header'>Stations pleines (aucune borne libre)</div>", unsafe_allow_html=True)
df_full = df[df["numdocksavailable"] == 0]
if df_full.empty:
    st.info("Aucune station pleine actuellement.")
else:
    st.dataframe(df_full[["stationcode", "name", "numbikesavailable", "arrondissement"]])

# === TABLEAU FINAL ===
st.markdown("<div class='section-header'>Toutes les stations disponibles</div>", unsafe_allow_html=True)
st.dataframe(df[[
    "stationcode", "name", "ebike", "mechanical", "numdocksavailable", "arrondissement"
]].sort_values("arrondissement"))
