# platformGameMobile

Gioco platform 2D a scorrimento per Android (nome provvisorio: **Robo Jump**), scritto in Java nativo senza librerie esterne.

- Livello 1 (2D): prato a scorrimento con gemme, casse bonus, nemici, burroni e traguardo.
- Livello 3 (2D, autunno): colline arrotondate, zucche al posto delle casse, massi e mattoni morbidi, burroni più larghi; nemici nuovi: riccio (spinoso, si travolge con la giravolta), corvo (vola a onde), ghianda (procede a balzi).
- Livello 4 (3D, autunno): bosco di latifoglie rosse, arancio e gialle, sentieri stretti, un ponte di una sola casella, massi e zucche arrotondati, bordi dei burroni morbidi, foglie e vento. Stessi tre nuovi nemici.
- Livello 5 (2D, aurora boreale): tundra di notte con aurora in cielo, neve e ghiaccio, mattoni di ghiaccio, burroni larghi con appigli di mattoni e pietre, molti corvi.
- Livello 7 (2D, incendio): una corsa per salvarsi dal rogo. Un muro di fuoco insegue il robot (velocità crescente, con una radura dove si ferma), getti di fiamma a tempo con avviso, ponti e torri di tavole bruciate che crollano mezzo secondo dopo il tocco, bivio tra terreno incandescente e torre, sedici caselle di lava da superare su tavole sospese.
- Livello 8 (3D, incendio): fuga nel bosco in fiamme. Muro di fuoco alle spalle (bagliore rosso e «FUOCO!» quando si avvicina), tronchi in fiamme che rotolano di traverso, slalom tra muri di tronchi con un solo varco, braciere con getti a scacchiera, ponte sulla lava attraversato dai tronchi, radura con la cella energetica, rogo finale tra due strisce di lava.
- Livello 9 (2D, primavera): ciliegi in fiore, tre fiumi larghi 10 caselle che si superano solo con il fiore a molla all'ultima casella della riva (il fiore lancia in alto e in avanti), due balconi di pietra raggiungibili dai fiori (uno con la cella energetica), pochi rovi isolati (palla viola con spine bianche).
- Livello 10 (3D, primavera): giardino di ciliegi con petali al vento; tre fiumi larghi 10 m con una fila di fiori a molla in riva, due balconi di pietra a 4 m con la cella energetica, due campi di rovi a zigzag con corsia libera. Meccaniche: fiore a molla e rovi (`Level.SPRING`, `Level.thorns`, `World3D.springs/thorns`).
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
- `tools/gen_fire.py` — immagini dei livelli con l'incendio (cielo di fumo, pineta in fiamme, terreno bruciato, lava, fiamme, braci).
- `tools/gen_spring.py` — immagini dei livelli di primavera (cielo, ciliegi in fiore, prato fiorito, acqua, fiore a molla, rovi, texture 3D).
- `tools/gen_aurora.py` — immagini dei livelli con l'aurora boreale (cielo, pineta, neve, ghiaccio, tende d'aurora, fiocchi).
- `tools/gen_gate.py` — portone d'arrivo dei livelli 2D.
- `tools/gen_foes.py` — texture dei nemici 3D (pelo, piume, guscio, scaglie, corazza); i modelli sono in `core3d/FoeModels.java`.
- `tools/preview/FoeShot.java` — primi piani dei nemici 3D per controllarli al PC.
- `core3d/Decor.java`: dettagli dei livelli 3D (funghi, tronchi caduti, ceppi, radici, cespugli, sassolini, neve, cristalli); `core3d/HeroModel.java`: il protagonista con zaino, veste bianca e mantello di stoffa ricalcolato a ogni fotogramma. Texture: `python3 tools/gen_decor.py`. Primi piani del protagonista: `tools/preview/HeroShot.java` (scrive `build/preview/hero.png`).
- Collisioni 3D: `World3D.enemyBlocked` fa dietrofront ai nemici davanti a blocchi, casse, muro laterale e burroni; `tools/preview/Check3.java` lo verifica per ogni nemico dei livelli 2, 4 e 6 (`java -Dlv=4 -Donly=1 -cp build/preview/classes Check3`). Sprite 2D del robot, dello scarabeo e degli altri nemici: `tools/gen_assets.py hero spin enemy` e `tools/gen_autumn.py au_enemies` (rigenerano anche l'icona dell'app).
- `tools/preview/check.sh` — controlli automatici sulla logica dei due livelli (compresi i bot che li completano).
- `tools/preview/scenes.sh` — salva schermate dei due livelli in `build/preview` (il 3D con un renderer software).

## Build

Ogni push su `main` compila l'APK con GitHub Actions e lo pubblica nella release `latest`:
<https://github.com/valeriodipaolo/platformGameMobile/releases/latest/download/platformGame.apk>
