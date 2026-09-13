# Third-Party Notices

RepoOnboard is licensed under the Apache License 2.0. The executable JAR also
contains third-party software, and the production Web UI contains bundled
third-party assets. Those components remain under their respective licenses.

This audit reflects the runtime Maven dependency tree and the non-development
packages in `frontend/package-lock.json` for RepoOnboard `0.1.0-SNAPSHOT` on
2026-09-13. Build-only and test-only tools are not distributed in the
executable JAR and are outside this distribution inventory.

## License texts

- Apache-2.0: [`LICENSE`](./LICENSE)
- EPL-2.0: [`third-party-licenses/EPL-2.0.txt`](./third-party-licenses/EPL-2.0.txt)
- MIT: [`third-party-licenses/MIT.txt`](./third-party-licenses/MIT.txt)
- BSD-2-Clause: [`third-party-licenses/BSD-2-Clause.txt`](./third-party-licenses/BSD-2-Clause.txt)
- BSD-3-Clause: [`third-party-licenses/BSD-3-Clause.txt`](./third-party-licenses/BSD-3-Clause.txt)
- ISC: [`third-party-licenses/ISC.txt`](./third-party-licenses/ISC.txt)

The same files are embedded in the executable JAR below
`META-INF/repoonboard/`.

## Java runtime components

| Component | Version | License | Upstream |
| --- | --- | --- | --- |
| `tools.jackson.core:jackson-databind` | 3.1.4 | Apache-2.0 | <https://github.com/FasterXML/jackson-databind> |
| `com.fasterxml.jackson.core:jackson-annotations` | 2.21 | Apache-2.0 | <https://github.com/FasterXML/jackson-annotations> |
| `tools.jackson.core:jackson-core` | 3.1.4 | Apache-2.0 | <https://github.com/FasterXML/jackson-core> |
| `com.github.javaparser:javaparser-core` | 3.28.2 | Apache-2.0 OR LGPL-3.0-or-later; distributed here under Apache-2.0 | <https://github.com/javaparser/javaparser> |
| `org.apache.maven:maven-model` | 3.9.16 | Apache-2.0 | <https://maven.apache.org/ref/3.9.16/maven-model/> |
| `org.apache.maven:maven-model-builder` | 3.9.16 | Apache-2.0 | <https://maven.apache.org/ref/3.9.16/maven-model-builder/> |
| `org.apache.maven:maven-artifact` | 3.9.16 | Apache-2.0 | <https://maven.apache.org/ref/3.9.16/maven-artifact/> |
| `org.apache.maven:maven-builder-support` | 3.9.16 | Apache-2.0 | <https://maven.apache.org/ref/3.9.16/maven-builder-support/> |
| `org.codehaus.plexus:plexus-utils` | 3.6.1 | Apache-2.0 | <https://github.com/codehaus-plexus/plexus-utils> |
| `org.codehaus.plexus:plexus-interpolation` | 1.29 | Apache-2.0 | <https://github.com/codehaus-plexus/plexus-interpolation> |
| `javax.inject:javax.inject` | 1 | Apache-2.0 | <https://github.com/javax-inject/javax-inject> |
| `org.eclipse.sisu:org.eclipse.sisu.inject` | 1.0.0 | EPL-2.0 | <https://github.com/eclipse-sisu/sisu-project> |
| `org.ow2.asm:asm` | 9.9.1 | BSD-3-Clause | <https://asm.ow2.io/> |
| `info.picocli:picocli` | 4.7.7 | Apache-2.0 | <https://github.com/remkop/picocli> |

Eclipse Sisu source code corresponding to the distributed component is
available from its upstream repository and the `1.0.0` tag:
<https://github.com/eclipse-sisu/sisu-project/tree/releases/1.0.0>.

ASM carries this copyright notice:

> Copyright (c) 2000-2011 INRIA, France Telecom. All rights reserved.

## Frontend production components

The Vue compiler packages are included because Vue's runtime compiler is part
of the current production dependency closure recorded by the lockfile.

| Component | Version | License |
| --- | --- | --- |
| `@babel/helper-string-parser` | 7.29.7 | MIT |
| `@babel/helper-validator-identifier` | 7.29.7 | MIT |
| `@babel/parser` | 7.29.8 | MIT |
| `@babel/types` | 7.29.8 | MIT |
| `@jridgewell/sourcemap-codec` | 1.6.0 | MIT |
| `@vue/compiler-core` | 3.5.42 | MIT |
| `@vue/compiler-dom` | 3.5.42 | MIT |
| `@vue/compiler-sfc` | 3.5.42 | MIT |
| `@vue/compiler-ssr` | 3.5.42 | MIT |
| `@vue/reactivity` | 3.5.42 | MIT |
| `@vue/runtime-core` | 3.5.42 | MIT |
| `@vue/runtime-dom` | 3.5.42 | MIT |
| `@vue/server-renderer` | 3.5.42 | MIT |
| `@vue/shared` | 3.5.42 | MIT |
| `csstype` | 3.2.3 | MIT |
| `cytoscape` | 3.34.3 | MIT |
| `entities` | 7.0.1 | BSD-2-Clause |
| `estree-walker` | 2.0.2 | MIT |
| `magic-string` | 0.30.21 | MIT |
| `nanoid` | 3.3.18 | MIT |
| `picocolors` | 1.1.1 | ISC |
| `postcss` | 8.5.28 | MIT |
| `source-map-js` | 1.2.1 | BSD-3-Clause |
| `vue` | 3.5.42 | MIT |

Copyright notices retained from the installed production packages:

- Babel helpers/types: Copyright (c) 2014-present Sebastian McKenzie and other contributors.
- Babel parser: Copyright (C) 2012-2014 by various contributors (see upstream AUTHORS).
- sourcemap-codec: Copyright 2024 Justin Ridgewell.
- Vue packages: Copyright (c) 2018-present, Yuxi (Evan) You.
- csstype: Copyright (c) 2017-2018 Fredrik Nicol.
- Cytoscape.js: Copyright (c) 2016-2026, The Cytoscape Consortium.
- entities: Copyright (c) Felix Böhm. All rights reserved.
- estree-walker: Copyright (c) 2015-20 its contributors.
- magic-string: Copyright 2018 Rich Harris.
- nanoid: Copyright 2017 Andrey Sitnik.
- picocolors: Copyright (c) 2021-2024 Oleksii Raspopov, Kostiantyn Denysov, Anton Verinov.
- PostCSS: Copyright 2013 Andrey Sitnik.
- source-map-js: Copyright (c) 2009-2011, Mozilla Foundation and contributors. All rights reserved.

Upstream package metadata and each installed package's `LICENSE` file were
used to verify the frontend license identifiers and notices. The lockfile is
authoritative for the versions distributed by this revision.
