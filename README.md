# platformGameMobile

Gioco platform 2D a scorrimento per Android (nome provvisorio: **Robo Jump**), scritto in Java nativo senza librerie esterne.

- Livello 1 (2D): prato a scorrimento con gemme, casse bonus, nemici, burroni e traguardo.
- Livello 3 (2D, autunno): colline arrotondate, zucche al posto delle casse, massi e mattoni morbidi, burroni più larghi; nemici nuovi: riccio (spinoso, si travolge con la giravolta), corvo (vola a onde), ghianda (procede a balzi).
- Livello 4 (3D, autunno): bosco di latifoglie rosse, arancio e gialle, sentieri stretti, un ponte di una sola casella, massi e zucche arrotondati, bordi dei burroni morbidi, foglie e vento. Stessi tre nuovi nemici.
- Livello 5 (2D, aurora boreale): tundra di notte con aurora in cielo, neve e ghiaccio, mattoni di ghiaccio, burroni larghi con appigli di mattoni e pietre, molti corvi.
- Livello 6 (3D, aurora boreale): lago ghiacciato sotto le tende dell'aurora che ondeggiano, abeti innevati, fiocchi di neve; lastre di ghiaccio, diagonale di fenditure, lago da attraversare di lastrone in lastrone, ponte a zig-zag e grotta segreta.
- Al posto della bandiera, nei livelli 2D c'è un portone di pietra che si apre quando si arriva alla fine (come nei livelli 3D).
- Livello 2 (3D, OpenGL ES 2.0): corridoio nel bosco visto da dietro le spalle. Cinque schemi di comando, da scegliere nel menu: joystick libero, joystick fisso, entrambi anche a 4 direzioni (senza diagonali), carro armato.
- Mosse: salto (più alto tenendo premuto), doppio salto, giravolta, planata.

## Struttura

- `app/src/main/java/.../core` — regole del gioco, livello e disegno della scena (Java puro, senza Android).
- `app/src/main/java/...` — parte Android: `MainActivity`, `GameView` (ciclo di gioco e tocchi), `AndroidPainter`.
- `app/src/main/res/drawable-nodpi` — grafica in PNG, generata da `tools/gen_assets.py`.
  Per usare una foto o un disegno al posto di un'immagine basta sostituire il PNG con lo stesso nome.
- `app/src/main/java/.../core3d` — livello 3D: regole (`World3D`), modelli e scena (`Scene3D`), matrici (Java puro).
- `GlRenderer`, `Level3DView` — disegno OpenGL e joystick del livello 3D; `MenuView` — scelta del livello.
- `tools/gen_autumn.py` — immagini dei livelli d'autunno (sfondi, terreno, blocchi, nemici, texture del bosco).
- `tools/gen_aurora.py` — immagini dei livelli con l'aurora boreale (cielo, pineta, neve, ghiaccio, tende d'aurora, fiocchi).
- `tools/gen_gate.py` — portone d'arrivo dei livelli 2D.
- `tools/gen_foes.py` — texture dei nemici 3D (pelo, piume, guscio, scaglie, corazza); i modelli sono in `core3d/FoeModels.java`.
- `tools/preview/FoeShot.java` — primi piani dei nemici 3D per controllarli al PC.
- `core3d/Decor.java`: dettagli dei livelli 3D (funghi, tronchi caduti, ceppi, radici, cespugli, sassolini, neve, cristalli); `core3d/HeroModel.java`: il protagonista con zaino, veste bianca e mantello di stoffa ricalcolato a ogni fotogramma. Texture: `python3 tools/gen_decor.py`. Primi piani del protagonista: `tools/preview/HeroShot.java` (scrive `build/preview/hero.png`).
- `tools/preview/check.sh` — controlli automatici sulla logica dei due livelli (compresi i bot che li completano).
- `tools/preview/scenes.sh` — salva schermate dei due livelli in `build/preview` (il 3D con un renderer software).

## Build

Ogni push su `main` compila l'APK con GitHub Actions e lo pubblica nella release `latest`:
<https://github.com/valeriodipaolo/platformGameMobile/releases/latest/download/platformGame.apk>
