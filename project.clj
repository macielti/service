(defproject net.clojars.macielti/service "0.1.0"

  :description "Service is a Pedestal service Integrant component"

  :url "https://github.com/macielti/service"

  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.12.4"]
                 [net.clojars.macielti/common-clj "46.0.0"]
                 [io.pedestal/pedestal.service "0.8.1"]
                 [com.vodori/schema-conformer "0.1.2"]
                 [io.pedestal/pedestal.jetty "0.8.1"]
                 [io.pedestal/pedestal.error "0.8.1"]
                 [org.clojure/tools.logging "1.3.1"]
                 [clj-commons/iapetos "0.1.14"]             ;;TODO Use prometheus-component dependency instead of iapetos
                 [siili/humanize "0.1.1"]
                 [integrant "1.0.1"]]

  :profiles {:dev {:resource-paths ^:replace ["test/resources"]

                   :test-paths     ^:replace ["test/unit" "test/integration" "test/helpers"]

                   :plugins        [[lein-cloverage "1.2.4"]
                                    [com.github.clojure-lsp/lein-clojure-lsp "2.0.13"]
                                    [com.github.liquidz/antq "RELEASE"]]

                   :dependencies   [[prismatic/schema "1.4.1"]
                                    [nubank/matcher-combinators "3.10.0"]
                                    [com.taoensso/timbre "6.8.0"]
                                    [hashp "0.2.2"]]

                   :injections     [(require 'hashp.core)]

                   :aliases        {"clean-ns"     ["clojure-lsp" "clean-ns" "--dry"] ;; check if namespaces are clean
                                    "format"       ["clojure-lsp" "format" "--dry"] ;; check if namespaces are formatted
                                    "diagnostics"  ["clojure-lsp" "diagnostics"]
                                    "lint"         ["do" ["clean-ns"] ["format"] ["diagnostics"]]
                                    "clean-ns-fix" ["clojure-lsp" "clean-ns"]
                                    "format-fix"   ["clojure-lsp" "format"]
                                    "lint-fix"     ["do" ["clean-ns-fix"] ["format-fix"]]}

                   :repl-options   {:init-ns service.component}}}

  :resource-paths ["resources"])
