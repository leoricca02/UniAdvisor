TODO

Deadline: Registration must be completed no later than 3 days before the scheduled exam date.

CHECK THESE Requirements:

1.Use at least one public Cloud service (e.g., from https://publicapis.io/)
2.Support multiple users with login and authentication
3.Include some 2D graphics
4. Use at least one sensor
5. Use GPS
6. Use the camera or perform image processing
7. Include concurrency (async tasks, coroutines, etc.)
8. Use at least one additional cloud feature (e.g., Google Cloud service)
9. Implement a REST API running on a remote server (e.g., PythonAnywhere, Docker on a VM)
10. Implement a storage service (e.g., a simple SQL database accessed via the REST API)

###### -Filtri (ordine) DONE



###### -Inserimento note non intuitivo, da mettere assieme al + dove si mettono le recensioni (possibilità scelta recensione e note) DONE



###### -Home screen, mettere "Home" in alto al posto di "UniAdvisor" DONE

###### 

###### -Aggiungere nella home in alto a destra il bottone di logout DONE



###### -Il tasto view note non me la fa visualizzare ma la scarica comunque, magari è meglio introdurre, se possibile, una funzionalità per vedere il file e poi decidere se scaricarlo DONE



###### -Non si vedono le varie reviews delle singole note, si vedono solo i voti DONE



###### -Cambiare la stellina ratings con Note Ratings (Home e User) DONE



###### -Sotto Academic, invece di leggere "Your faculty" leggere il nome della facoltà DONE



###### -Non è chiaro come dare il voto a una nota, per me bisogna aggiungere un tastino blu "Rate" sotto alla valutazione DONE

###### 

###### -Nel momento in cui cambio la faculty dal mio Profilo, continua a fare caching dei corsi della facoltà precedente DONE

###### 

###### -Nelle "my notes" levare il course id e mettere il nome del corso, levare anche Note #..., mettere solo "Note" DONE



###### -Camera + OCR (1-2 days)

###### 1\. Add CameraX for photo capture

###### 2\. Integrate ML Kit Text Recognition

###### 3\. Create OCR preview screen

###### 4\. Generate searchable PDF from extracted text

###### 5\. Upload to Firebase Storage DONE



###### -Sensors

###### 1\. Add shake detection for refresh

###### 2\. Implement tilt-based navigation (optional)

###### 3\. Add haptic feedback DONE



###### -Position

1. ###### Navigate to Class 🧭

######         "Navigate to this faculty" button on FacultyMainScreen

######  	"Navigate to this course" button on CourseDetailScreen

######  	Open Google Maps/Waze for turn-by-turn navigation

######  	Show building entrance and room number DONE



##### -Mettere tasto "Browse faculties", come funzionalità extra, per esplorare le altre facoltà senza dover cambiare la facoltà dal "Profilo" DONE



###### -Mettere stelline di media del voto al posto del voto nella schermata "Faculty" che mostra tutti i corsi        DONE

###### 

###### -Aggiungere la possibilità di vedere anche la "mezza stellina"      DONE

###### 

###### -Aggiungere l'obbligo di vedere il nome di chi ha caricato la nota

###### 

###### -Coerenza con rating vicino alle stelline in tutti i punti dell'app (1.5 e non 1.5/5)       DONE

###### 

###### -In my reviews, mettere nome corso e non "Course #1"        DONE

###### 

###### -Aggiungere una matita per editare, ed eventualmente cancellare, le proprie note reviews

###### 

###### -rimuovere i timestamp dalle cose caricate, lasciare solo la data       DONE

###### 

###### -Mettere i colori nelle note, quindi rosso se è basso, giallo se nella media, verde se è top.       DONE



###### -quando registro un utente, scelgo la facoltà e vado su "Your Faculty" ancora sta su "Select Faculty - No faculty selected, please select  faculty in your profile". Se la cambio e poi la ricambio dal mio profilo allora funziona. DONE



###### -aggiungere filtro alle reviews delle note DONE



###### -feature aggiuntiva: inserire una barra di ricerca per cercare i corsi DONE



-testare shake features



###### -in fase di registrazione, non funziona bene la scelta dell'anno di nascita, anche se scelgo un anno tipo "2004" mi seleziona "7", oppure se scelgo "2002" mi seleziona "9" e così via DONE



###### -modificare la "course page" --> è sviluppata troppo verticalmente, aggiungere due shortcut dopo gli average rating del corso "See Course Reviews" e "See Course Notes" DONE

<<<<<<< HEAD


-Magnetometer \& Accelerometer/Gyroscope (Provide an accurate on-campus navigation tool. Users can hold their phone flat, and a compass overlay points them to their next class or a specific building. The PDF mentions using the gyroscope and accelerometer for orientation)



-Ambient Light Sensor (Automatically switch your in-app note or PDF viewer (PdfViewerActivity.kt) between light mode and a comfortable dark mode based on the ambient light level.)



-new feature --> how many people there are in a room
=======
-vedere se è possibile introdurre il nome di chi ha scritto la note quando si visualizzano le note di un corso
>>>>>>> origin/main

