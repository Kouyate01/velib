import json, requests

def get_arrondissement(lat, lon, token):
    url = f"https://api.mapbox.com/geocoding/v5/mapbox.places/{lon},{lat}.json"
    resp = requests.get(url, params={
        "access_token": token,
        "types": "postcode"
    }, timeout=5)
    feats = resp.json().get("features", [])
    return feats[0]["text"] if feats else "Unknown"

with open("station_information.json","r",encoding="utf-8") as f:
    stations = json.load(f)["data"]["stations"]

mapping = {st["name"]: get_arrondissement(st["lat"], st["lon"], "TA_CLE_MAPBOX")
           for st in stations}

with open("name_to_departement.py","w",encoding="utf-8") as f:
    f.write("name_to_departement = {\n")
    for name, cp in mapping.items():
        f.write(f'    "{name}": "{cp}",\n')
    f.write("}\n")
print("✅ Fichier avec codes postaux prêt.")
