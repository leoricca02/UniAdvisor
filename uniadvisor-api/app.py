# file: app.py

from flask import Flask, jsonify, request

app = Flask(__name__)

# Dati finti per simulare un database
DUMMY_COURSES = {
    "ingegneria": [
        {"id": "ing-01", "name": "Analisi Matematica I", "professor": "M. Rossi"},
        {"id": "ing-02", "name": "Fisica Generale", "professor": "L. Bianchi"},
        {"id": "ing-03", "name": "Mobile Applications and Cloud Computing", "professor": "R. Beraldi"}
    ],
    "lettere": [
        {"id": "let-01", "name": "Letteratura Italiana", "professor": "G. Verdi"},
        {"id": "let-02", "name": "Filologia Romanza", "professor": "A. Neri"}
    ]
}

@app.route('/')
def home():
    return "Benvenuto sull'API di UniAdvisor!"

@app.route('/courses/recommendations', methods=['GET'])
def get_recommendations():
    faculty = request.args.get('faculty')

    if not faculty:
        return jsonify({"error": "Parametro 'faculty' mancante"}), 400
    
    # Logica di raccomandazione molto semplice: restituisce il primo corso della lista
    courses_for_faculty = DUMMY_COURSES.get(faculty.lower(), [])
    
    if not courses_for_faculty:
         return jsonify({"error": f"Nessun corso trovato per la facoltà {faculty}"}), 404

    # Simuliamo di raccomandare il corso con il nome più lungo
    recommended_course = max(courses_for_faculty, key=lambda x: len(x['name']))

    return jsonify([
        {"id": recommended_course['id'], "name": recommended_course['name'], "reason": "Corso più popolare del mese"}
    ])

# --- Come avviare il server ---
# 1. Apri il terminale (o Prompt dei comandi) nella cartella 'uniadvisor-api'.
# 2. Installa le dipendenze: pip install -r requirements.txt
# 3. Avvia il server: python app.py
# 4. Il server sarà in ascolto su http://127.0.0.1:5000/
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)