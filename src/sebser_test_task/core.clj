(ns sebser-test-task.core
  (:require [reitit.ring :as ring]
            [instaparse.core :as insta]
            [ring.adapter.jetty9 :refer [run-jetty]]
            [reitit.ring.middleware.parameters :as parameters]))

(def ariphm-expr
  (insta/parser
    "S = <#'\\s*'> expr <#'\\s*'>
     expr =  operand ( <#'\\s*'> operator <#'\\s*'> operand )*
     operator = '+'|'-'|'*'|'/'
     operand = #'(-?(([1-9][0-9]*[.][0-9]*)|(0?[.][0-9]*)|([1-9][0-9]*[.]?)))' | <'('> <#'\\s*'> expr <#'\\s*'> <')'>
     "))

(def ariphm-expr2
  (insta/parser
    "S = <#'\\s*'> expr2 <#'\\s*'>
     expr1 =  operand ( <#'\\s*'> operator1 <#'\\s*'> operand )*
     expr2 =  operand ( <#'\\s*'> operator2 <#'\\s*'> operand )*
     operator1 = '*'|'/'
     operator2 = '+'|'-'
     operand = expr1 | #'(-?(([1-9][0-9]*[.][0-9]*)|(0?[.][0-9]*)|([1-9][0-9]*[.]?)))' | <'('> <#'\\s*'> expr2 <#'\\s*'> <')'>
     "))

(defn interpret
  [parsed-expr]
  (->> parsed-expr
       rest
       (cons [:operator "+"])
       (partition 2)
       (reduce
         (fn [acc [[_ operator] [_ operand]]]
           (let [operator-fn   (case operator
                                 "+" +
                                 "-" -
                                 "*" *
                                 "/" /)
                 operand-value (if (string? operand)
                                 (Double/parseDouble operand)
                                 (interpret operand))]
             (operator-fn acc operand-value)))
         0)))

(comment

  (interpret (second (ariphm-expr2 "1 + 2 * ( 3 + 4 )")))
  (interpret (second (ariphm-expr "1+2*(3+4)")))
  (interpret (second (ariphm-expr2 "1+2*(3+4)")))
  (interpret (second (ariphm-expr2 "1")))

  )

(defn calc-handler
  [req]
  (if-let [expr (some-> req :query-params (get "expr") not-empty)]
    (let [parsed-expr (ariphm-expr2 expr)
          res (interpret (second parsed-expr))]
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

(def srv nil)

(comment

  (do
    (when (some? srv)
      (.stop srv))
    (def srv (start-sebser-service 7000)))

  )