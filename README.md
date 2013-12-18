# WebRCP

WebRCP is a framework for launching Eclipse RCP-Applications with Java Web Start. WebRCP supports the most common platforms.

## Features

* Download and unpack your generated RCP archive
* Launch the given Eclipse RCP Product file

## Getting Started

* Import the project into your eclipse workspace
* Write your own property file or pass in the properties from your given build system
* Generate the jar file (and the index.html, plus *.jsp if needed)
* Deliver the generated files with your server

## Serving custom properties
Custom properties in your *.jnlp or *.jsp file, should be marked as "jnlp.custom.".
WebRCP than removes the jnlp.custom prefix and sets property as a new Systemproperty.

## Miscellaneous

The project was formerly hosted on http://sourceforge.net/projects/webrcp/
