# nuxeo-es-synonyms
===================

This plug-in enables the administration of synonyms through the admin interface



## List of Features

- Elasticsearch analyzer which use the synonyms filter
- Nuxeo operations to read and write synonyms and to run a reindexation of Elasticsearch
- A UI based on Web Components to visualize and edit synonyms through the admin interface

**Important: if you're running ElasticSearch as a standalone you need to do 2 things.**

1) Copy the file nuxeo-es-synonyms/nuxeo-es-synonyms-mp/src/main/resources/install/templates/es/config/synonyms.txt into the folder etc/elasticsearch/

2) Add an entry in the nuxeo.conf file for the property org.nuxeo.synonyms.path= /the/path/to/the/synonyms.txt

If you want to enable autoreindexation of ElasticSearch everytime the synonyms change you need to add an entry in the nuxeo.conf file for the property org.nuxeo.synonyms.autoreindex=true

## Build
### Install dependencies

#### Quick-start (for experienced users)

With Node.js installed, run the following one liner from the root of your Polymer Starter Kit download:

```sh
npm install -g gulp bower && npm install && bower install
```

#### Prerequisites (for everyone)

The full starter kit requires the following major dependencies:

- Node.js, used to run JavaScript tools from the command line.
- npm, the node package manager, installed with Node.js and used to install Node.js packages.
- gulp, a Node.js-based build tool.
- bower, a Node.js-based package manager used to install front-end packages (like Polymer).

**To install dependencies: (from command line)**

1)  Check your Node.js version.

```
node --version
```

The version should be at or above 4.2.3.

2)  If you don't have Node.js installed, or you have a lower version, go to [nodejs.org](https://nodejs.org) and click on the big green Install button.

3)  Install `gulp` and `bower`.

```
npm install -g gulp
npm install bower
```

This lets you run `gulp` and `bower` from the command line.

4)  Install the app's local `npm` and `bower` dependencies.

```
cd nuxeo-es-synonyms/nuxeo-es-synonyms-ui
npm install
bower install
```

This installs the element sets (Paper, Iron, Platinum) and tools the starter kit requires to build and serve apps.

Assuming maven is correctly setup on your computer go under the nuxeo-es-synonyms folder and run:

```
mvn install
```

## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/ep) and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/dam) and [case management](http://www.nuxeo.com/en/products/case-management). Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information at <http://www.nuxeo.com/>
# nuxeo-graphviz
