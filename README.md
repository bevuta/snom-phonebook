
# Phonebook for snom phones

## Purpose

Backend for snom phones to query various data sources by using the
minibrowser-feature. Expandable with own handler plugins. Licensed
under a BSD-2-Clause License.

## Quick-Start

To run this application, you need at least a JRE, or better an up and
running leiningen installation. For information about running clojure
projects, see https://github.com/technomancy/leiningen

Copy the example-configuration.clj and make it fitting your needs.

    cp example-configuration.clj configuration.clj
    $EDITOR configuration.clj

Run with `lein run` if you have installed leiningen.

If you don't want to care about leiningen and clojure, use the
provided jar and execute it with

    java -jar snom-phonebook-0.1.0-SNAPSHOT-standalone.jar

You need to have a configuration.clj staying in the directory from
where you called `java -jar ...`.

To configure your snom phone to make it use of this application, see
http://wiki.snom.com/Category:HowTo:Minibrowser#Keypad-_Triggered and
put in the input field whatever you've put in your configuration as
*base-url*.

## Handlers

### LdapHandler

Queries an existing LDAP Server. Searches in the "cn"-field and
returns the phone-number from "telephoneNumber".

Parameters:

* *server* – IP or Hostname for the LDAP Server
* *basedn* – base dn for search
* *prefix* – prefix to add to retrieved phone numbers

**Example:**

    {:type :ldap
     :server "ldap://ldap.virginia.edu"
     :basedn "o=University of Virginia,c=US"
     :prefix "0"}

### MySQLHandler

Queries a MySQL-Database. You can define from which fields the
name-string is built.

Parameters:

* *database-configuration*
  * classname – com.mysql.jdbc.Driver
  * subprotocol – mysql
  * subname – //ip-or-hostname:port/databasename
  * user
  * password
* *table* – where the data is stored
* *prefix* – prefix to add to retrieved phone numbers
* *name_elements* – list with all needed columns to build the name-string
* *search_elements* – list with elements in which columns should be searched (e.g. name and first name)
* *phone_element* – string with name of the column with the phone number
* *string* – formatstring (actually a vector), from which the name-string will be built.  write down the column name to print the content of that column. write "!str" to write "str" into that string.  write ">str" to write str, if a record comes before the next static ("!..") element useful e.g. to seperate first and last name by a comma only if there is a forename.

**Example:**

    {:type :mysql
     :database-configuration
     {:classname "com.mysql.jdbc.Driver"
      :subprotocol "mysql"
      :subname "//localhost:3306/groupoffice"
      :user "go"
      :password "x"}
     :table "ab_contacts"
     :name-elements ["first_name" "middle_name" "last_name"]
     :search-elements ["first_name" "last_name"]
     :phone-element "work_phone"
     :format ["last_name" ">, " "first_name" "> " "middle_name" "! (Work) (" "work_phone" "!)"]
     :prefix "0"}

### FlatfileHandler

Parses a file with regexp for name and number and returns them.

Parameters:

* *file* – filename to parse
* *regular-expression* – regular expression which returns two strings out of a line. examples see below
* *name-field* – column-id for phone number
* *number-field* – column-id for phone number
* *prefix* – prefix to add to retrieved phone numbers

**Example for parsing asterisk voicemail-config:**

    {:type :flatfile
     :regular-expression #"^(\d+) => \d*\,([^,]+)?,"
     :file "/etc/asterisk/voicemail-voiceboxes.conf"
     :prefix "0"
     :number-field 0
     :name-field 1}

To use this, you have to outsource the voicebox-config from the
voicemail.conf:

    (...)
    
    [default]
    #include /etc/asterisk/voicemail-voiceboxes.conf

Format for voicemail-voiceboxes.conf is:

    101 => 1234,User A,user-a@example.com
    102 => 1234,User B,user-b@example.com,user-b-notify@example.com

# Contact

http://www.bevuta.com/ - bevuta IT GmbH

If you have written your own handler modules, found bugs, have an
improvement suggestion, don't hesitate to contact us ;)

# Copyright

Copyright (c) 2015, bevuta IT GmbH

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

1. Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
