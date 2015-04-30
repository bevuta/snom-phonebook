{:country-prefix "+49"
 :port 3000
 :base-url "http://web-server-ip-here:3000/"
 :handlers
 [
  ;; ldap
  {:type :ldap
   :server "ldap.uni-erlangen.de"
   :basedn "o=Universitaet Erlangen-Nuernberg,c=DE"
   :prefix "0"}
  ;; mysql
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
  ;; flatfile
  {:type :flatfile
   :regular-expression #"^(\d+) => \d*\,([^,]+)?,"
   :file "/etc/asterisk/voicemail-voiceboxes.conf"
   :prefix "0"
   :number-field 0
   :name-field 1}
  ]}
