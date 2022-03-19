(ns sebser-test-task.core
  (:require [reitit.ring :as ring]
            [instaparse.core :as insta]
            [instaparse.transform :as intra]
            [ring.adapter.jetty9 :refer [run-jetty]]
            [reitit.ring.middleware.parameters :as parameters]))

(def ariphm-expr
  (insta/parser
    "<S> = <#'\\s*'> expr2 <#'\\s*'>
     expr1 =  operand ( <#'\\s*'> operator1 <#'\\s*'> operand )*
     expr2 =  operand ( <#'\\s*'> operator2 <#'\\s*'> operand )*
     operator1 = '*'|'/'
     operator2 = '+'|'-'
     operand = expr1 | #'(-?(([1-9][0-9]*[.][0-9]*)|(0?[.][0-9]*)|([1-9][0-9]*[.]?)))' | <'('> <#'\\s*'> expr2 <#'\\s*'> <')'>
     "))

(declare interpret)

(defn operand-interpreter
  [operand]
  (cond
    (string? operand) (Double/parseDouble operand)
    (vector? operand) (interpret operand)
    :else operand))

(defn operator-interpreter
  [operator]
  (let [operator (case operator
                   "*" *
                   "/" /
                   "+" +
                   "-" -)]
    operator))

(defn expr-interpreter
  [& tokens]
  (->> tokens
      (cons +)
      (partition 2)
      (reduce
        (fn [acc [operator-fn operand-value]]
          (operator-fn acc operand-value))
        0)))

(defn interpret
  [parsed-expr]
  (intra/transform
    {:operand   operand-interpreter
     :operator1 operator-interpreter
     :operator2 operator-interpreter
     :expr1     expr-interpreter
     :expr2     expr-interpreter}
    parsed-expr))

(comment

  (interpret (first (ariphm-expr "1 + 2 * ( 3 + 4 )")))
  (interpret (first (ariphm-expr "1+2*(3+4)")))
  (interpret (first (ariphm-expr "1")))

  )

(defn calc-handler
  [req]
  (if-let [expr (some-> req :query-params (get "expr") not-empty)]
    (let [parsed-expr (ariphm-expr expr)
          res (interpret (first parsed-expr))]
      {:status  200
       :headers {"content-type" "text/plain"}
       :body    (str "Input: " expr "\n"
                     "Parsed expression: " parsed-expr "\n"
                     "Result: " res)})
    {:status 400
     :headers {"content-type" "text/plain"}
     :body   (str "Provide expr query parameter")}))

(def app
  (ring/ring-handler
    (ring/router
      ["/calc" {:get {:handler calc-handler}}]
      {:data {:middleware [parameters/parameters-middleware]}})))

(defn start-sebser-service
  [port]
  (run-jetty app {:port  port
                  :join? false}))

(defn -main
  [& [port]]
  (let [port (or (some-> port not-empty Integer/parseInt) 7000)]
    (start-sebser-service port)))



(comment
  (def srv nil)

  (do
    (when (some? srv)
      (.stop srv))
    (def srv (start-sebser-service 7000)))

  )