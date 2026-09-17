# Aqua Server
Multipurpose game server powered by Spring Boot, for ALL.Net based games

This is a forked maintaining attempt of the [original Aqua server](https://dev.s-ul.net/NeumPhis/aqua)

### Supported Games
* CHUNITHM SUN (and below)
* CHUNITHM Paradise Lost (and below)
* Maimai DX Festival (and below)
* Card Maker (1.34)
* Project DIVA Arcade Future Tone
* O.N.G.E.K.I. bright memory (and below)

Check out these docs for more information.
* [Game specific notes](docs/game_specific_notes.md)
* [Frequently asked questions](docs/frequently_asked_questions.md)
* [Changelog](CHANGELOG.md)

### Repository layout
This workspace contains two separate repositories:

* `aqua-master` - this server
* `aqua-viewer` - the WebUI (AquaViewer), served by this server under `/web/`

### Web UI
AquaViewer is embedded into the jar and served under `/web/`, for example `http://localhost/web/`. No extra setup is required.

If you want to use your own build instead, put AquaViewer files into a `web` folder next to the jar. That folder takes priority over the embedded copy.

Serving can be turned off entirely with `aquaviewer.server.enable=false`.

### Notes
* Some game may require additional patches and these will not provided in this project and repository. You already found this, so you know where to find related resources too.
* This repository may contain untested, experimental implementation for few games which I can't test properly. If you couldn't find your wanted game in the above list, do not expect support.
* This server also provides a simple API for viewing play records and edit settings for some games.
* The login page can list cards registered on the server (the card picker). Since this exposes access codes, it only works from the local network. Remote clients are rejected and must type the access code manually.

### Usage
Requirements:
* Java 17 or above (for running JAR)
* Optional databases: MariaDB 10.6.x (recommended) or MySQL 8.0.x

Run `java -jar aqua-x.x.xx-RELEASE.jar`

Once started, the management UI is reachable at `http://<your-host>/web/`.

By default, Aqua will use sqlite and save user data in data/db.sqlite.

If you want to use optional databases, edit configuration file then it will auto create the table and import some initial data.

### Configuration
Configuration is saved in `config/application.properties`, spring loads this file automatically.

* The host and port of game title servers can be overritten in `allnet.server.host` and `allnet.server.port`. By default it will send the same host and port the client used the request this information.
This will be send to the game at booting and being used by following request.
* You can switch to MariaDB (or MySQL) database by commenting the Sqlite part.
* For some game, you might need to change some game specific config entries.
* HTTP responses are compressed (gzip) by default. See the `server.compression.*` entries to tune or disable it.

### Building
You need to install JDK 17 on your system. However, you don't need to care about Gradle, as wrapper script is included.

The WebUI is packaged into the jar, so it has to be built first:
```
cd ../aqua-viewer
npm install
npm run build

cd ../aqua-master
gradlew clean build
```
The `build/libs` folder will contain an jar file.

Any jar build requires `../aqua-viewer/dist/aqua-viewer` to exist. If it is missing, the build stops with a message telling you to run `npm run build` first.

To only package an existing UI build into a single executable jar, use:
```
gradlew bootJarWithUi
```

### Credit
* **samnyan**: The creator and developer of the original Aqua server
* **Akasaka Ryuunosuke** : providing all the DIVA protocol information
* Dom Eori : Developer of forked Aqua server, from v0.0.17 and up
* All devs who contribute to the [MiniMe server](https://dev.s-ul.net/djhackers/minime)
* All contributors by merge request, issues and other channels
