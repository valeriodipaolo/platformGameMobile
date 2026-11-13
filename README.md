# platformGameMobile

Gioco platform 2D a scorrimento per Android (nome provvisorio: **Robo Jump**), scritto in Java nativo senza librerie esterne.

- Un livello: monete, casse bonus, nemici da schiacciare, burroni, traguardo con bandiera.
- Comandi a schermo: frecce a sinistra, salto a destra (tenendo premuto si salta più in alto).

## Struttura

- `app/src/main/java/.../core` — regole del gioco, livello e disegno della scena (Java puro, senza Android).
- `app/src/main/java/...` — parte Android: `MainActivity`, `GameView` (ciclo di gioco e tocchi), `AndroidPainter`.
- `app/src/main/res/drawable-nodpi` — grafica in PNG, generata da `tools/gen_assets.py`.
  Per usare una foto o un disegno al posto di un'immagine basta sostituire il PNG con lo stesso nome.
- `tools/preview/run.sh` — sul PC fa giocare il livello a un bot e salva delle schermate in `build/preview`.

## Build

Ogni push su `main` compila l'APK con GitHub Actions e lo pubblica nella release `latest`:
<https://github.com/valeriodipaolo/platformGameMobile/releases/latest/download/platformGame.apk>
