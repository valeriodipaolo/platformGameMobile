# platformGameMobile

Gioco platform 2D a scorrimento per Android (nome provvisorio: **Robo Jump**), scritto in Java nativo senza librerie esterne.

- Livello 1 (2D): prato a scorrimento con gemme, casse bonus, nemici, burroni e traguardo.
- Livello 2 (3D, OpenGL ES 2.0): corridoio nel bosco visto da dietro le spalle. Cinque schemi di comando, da scegliere nel menu: joystick libero, joystick fisso, entrambi anche a 4 direzioni (senza diagonali), carro armato.
- Mosse: salto (più alto tenendo premuto), doppio salto, giravolta, planata.

## Struttura

- `app/src/main/java/.../core` — regole del gioco, livello e disegno della scena (Java puro, senza Android).
- `app/src/main/java/...` — parte Android: `MainActivity`, `GameView` (ciclo di gioco e tocchi), `AndroidPainter`.
- `app/src/main/res/drawable-nodpi` — grafica in PNG, generata da `tools/gen_assets.py`.
  Per usare una foto o un disegno al posto di un'immagine basta sostituire il PNG con lo stesso nome.
- `app/src/main/java/.../core3d` — livello 3D: regole (`World3D`), modelli e scena (`Scene3D`), matrici (Java puro).
- `GlRenderer`, `Level3DView` — disegno OpenGL e joystick del livello 3D; `MenuView` — scelta del livello.
- `tools/preview/check.sh` — controlli automatici sulla logica dei due livelli (compresi i bot che li completano).
- `tools/preview/scenes.sh` — salva schermate dei due livelli in `build/preview` (il 3D con un renderer software).

## Build

Ogni push su `main` compila l'APK con GitHub Actions e lo pubblica nella release `latest`:
<https://github.com/valeriodipaolo/platformGameMobile/releases/latest/download/platformGame.apk>
