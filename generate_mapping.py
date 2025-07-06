import json
import pandas as pd

# Charger le JSON téléchargé
with open("station_information.json", "r", encoding="utf-8") as f:
    data = json.load(f)

# Extraire la liste des stations
stations = data["data"]["stations"]
df = pd.DataFrame(stations)

# Afficher les colonnes disponibles
print("Colonnes disponibles :", df.columns.tolist())

# Garder les codes + noms
df["stationCode"] = df["stationCode"].astype(str)
mapping = dict(zip(df["stationCode"], df["name"]))

# Sauvegarder pour annotation manuelle ensuite
with open("station_name_mapping.py", "w", encoding="utf-8") as out:
    out.write("stationcode_to_name = " + json.dumps(mapping, indent=2, ensure_ascii=False))

print("✅ Mapping stationcode_to_name sauvegardé dans station_name_mapping.py")
