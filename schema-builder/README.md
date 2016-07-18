Copyright 2016 Crown Copyright

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


Schema Builder
============
This module contains a schema builder UI.

There are two options for building and then running it:

Option 1 - Deployable war file
==============================

If you wish to deploy the war file to a container of your choice, then use this option.

To build the war file along with all its dependencies then run the following command from the parent directory:
'mvn clean install'

To deploy it to a server of your choice, take target/schema-builder.war and deploy as per the usual deployment process for your server.


Option 2 - Build using the standalone-schema-builder profile
=============================================

The application can be built and then run as a basic executable standalone war file from Maven. When run in this format, the default schemas represent the Film/Viewings example and the store used is a MockAccumuloStore.

To build it and its dependencies, use the following command from the parent directory:

'mvn clean install -P standalone-schema-builder'
This uses the 'standalone-schema-builder' profile to run jetty with the schema-builder project after it and its dependencies have been built.

This should launch an embedded jetty container, which can then be accessed via your browser pointing to the following url:
http://localhost:8080/schema-builder/

