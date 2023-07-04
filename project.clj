(defproject link.lmnd/yandex-music "0.1.2"
  :description "Some Yandex Music API methods"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [ [org.clojure/clojure "1.11.1"]
                  [clj-http "3.12.3"]
                  [org.clj-commons/digest "1.4.100"]
                  [org.craigandera/dynne "0.4.1"]
                  [cheshire             "5.10.0"]
                 ]
  :repl-options {:init-ns yandex-music.core})
