# PacheteScan

**PacheteScan** este o aplicatie Android insotitoare pentru o platforma de curierat/tracking colete. Permite personalului din depozit si livratorilor sa scaneze codul de bare al unui colet, sa vada intregul istoric al acestuia si sa inregistreze rapoarte de deteriorare sau sesizari de informatii lipsa direct din teren — inclusiv poze.

Aplicatia e construita in jurul **scannerelor cu Honeywell AIDC** (prin Honeywell AIDC SDK) pentru citiri rapide, declansate hardware, dar functioneaza si pe orice dispozitiv Android standard. Pe hardware non-Honeywell, capturarea codului de bare trece pe introducere manuala a AWB-ului in loc de scanare hardware, deci aplicatia ramane utilizabila — doar fara trigger-ul dedicat de scanare si performanta de citire continua a terminalelor Honeywell.

## Ce face

- **Scaneaza sau introdu manual un cod de colet** pentru a vedea tot ce se stie despre el intr-un singur ecran: detalii pachet, greutate, adrese, si lista completa de rapoarte de deteriorare si sesizari de informatii lipsa asociate, fiecare cu pozele atasate.
- **Inregistreaza un raport de deteriorare** pentru un pachet scanat, cu o coada nelimitata de poze atasate inainte de trimitere — faci cate poze e nevoie, vezi previzualizari mici, elimini oricare prin apasare lunga, apoi trimiti o singura data.
- **Inregistreaza o sesizare de informatii lipsa** (ex. o eticheta ilizibila sau AWB lipsa), cu o singura poza obligatorie a etichetei/problemei.
- **Ataseaza poze in mod fiabil**: fiecare poza intra intai intr-o coada locala, se incarca dupa ce inregistrarea parinte e creata, si se reincearca automat de cateva ori la esecuri tranzitorii de retea inainte sa ceara interventia userului.
- **Rasfoieste pozele ca thumbnail-uri** oriunde apar, cu vizualizare la rezolutie completa la apasare — thumbnail-urile sunt generate pe server, deci aplicatia nu trebuie sa descarce imagini complete doar ca sa arate o previzualizare.
- **Autentificare o singura data**: un login bazat pe JWT persista intre sesiuni, cu tokenul atasat automat la fiecare apel API.

## Arhitectura

Aplicatia urmeaza un pattern consecvent, scris manual, pe toate functionalitatile (autentificare, pachete, rapoarte de deteriorare, sesizari de informatii lipsa, incarcare imagini), fara sa se bazeze pe o librarie de retea sau DI:

```
Activity  →  ViewModel  →  Repository  →  DataSource  →  REST API
                ↑                              |
             LiveData  ←──────────────── Result<T> (Success / Error)
```

- **DataSource** — o clasa per functionalitate, vorbeste direct cu server-ul prin `HttpURLConnection` (inclusiv `multipart/form-data` construit manual pentru incarcarea pozelor), parseaza raspunsurile JSON cu `org.json`, si mapeaza codurile de status HTTP la rezultate tipizate.
- **Repository** — detine un `ExecutorService` de fundal, muta munca de pe thread-ul UI, si posteaza rezultatele inapoi printr-un `Handler` pe main looper. Implementat ca singleton thread-safe.
- **ViewModel** — expune `LiveData<Result>` catre UI si medieaza intre Activitati si Repository-uri; unele fluxuri expun si o cale directa prin callback, pentru operatii secventiale pe loturi (ex. incarcarea unei cozi de poze, una cate una, cu retry).
- **Activity** — Android Views obisnuite (fara Compose), CameraX pentru capturarea pozelor, si Honeywell AIDC SDK pentru evenimentele de scanare, acolo unde e disponibil.

Nu se foloseste nicio librarie de JSON, client HTTP, sau framework de DI — totul e scris direct pe biblioteca standard Android/Java plus SDK-ul Honeywell, ca sa se pastreze un consum redus de resurse pe hardware-ul de scanner portabil.

## Module principale

| Pachet | Responsabilitate |
|---|---|
| `data.*` | `DataSource` + `Repository` + modele de request/response, per functionalitate |
| `ui.*` | `ViewModel`, `Result`, si `ViewModelFactory`, per functionalitate |
| Activitati (pachetul radacina) | Ecrane: login, meniu, cautare pachet, raport deteriorare, sesizare informatii lipsa, captura poza |
| `ui.imagini` | Fluxul comun de poze: predarea capturii, incarcarea cu retry pe coada, incarcarea thumbnail-urilor |

## Fluxul de poze

Capturarea pozei e separata de incarcare:

1. Un ecran dedicat de captura face o poza cu CameraX si preda fisierul local ecranului care l-a apelat — nu incarca nimic singur.
2. Ecranul apelant (deteriorare / informatii lipsa) pune fisierul in coada locala, arata un thumbnail local decodat eficient (redus la rezolutie mica, fara sa incarce vreodata un bitmap complet doar pentru o previzualizare), si permite eliminarea pozelor din coada inainte de trimitere.
3. Odata ce inregistrarea parinte (raport de deteriorare / sesizare informatii lipsa) e creata cu succes, pozele din coada se incarca secvential, fiecare cu cateva reincercari silentioase la esec, inainte de a afisa un dialog de esec partial cu optiune de reincercare manuala.
4. Thumbnail-urile generate pe server (o copie redusa separata, creata la momentul incarcarii) sunt folosite oriunde apar poze in liste, pastrand consumul de date scazut; originalul la rezolutie completa se aduce doar la cerere.

## Backend

PacheteScan comunica cu un API ASP.NET Core Web API insotitor (partajat cu interfata web a platformei) prin endpoint-uri REST autentificate cu JWT, pentru autentificare, cautare pachete, creare deteriorari/sesizari informatii lipsa, si incarcare/citire imagini.

## Cerinte

- Dispozitiv Android sau scanner compatibil Honeywell AIDC, cu o versiune Android suportata
- Acces la retea catre API-ul backend
- Permisiune de camera (pentru capturarea pozelor)
- Pentru functionalitatea completa de scanare: un scanner Honeywell cu serviciul AIDC disponibil

## Status

In dezvoltare activa, in paralel cu platforma backend. Unele functionalitati planificate (ex. coada de poze offline cu retry pe fundal prin WorkManager) sunt inca in faza de design.
