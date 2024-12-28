(ns effective-agents-clj.workflows.routing
  (:require [instructor-clj.core :as i]
            [clojure.string :as cs]))

;;                     ┌─────────┐
;;                 ┌──>│ LLM 1   │──┐
;;                 │   └─────────┘  │
;; ┌──┐   ┌──────┐ │   ┌─────────┐  │   ┌───┐
;; │In│──>│Router│-┼-->│ LLM 2   │--└-->│Out│
;; └──┘   └──────┘ │   └─────────┘      └───┘
;;                 │   ┌─────────┐       /
;;                 └-->│ LLM 3   │------'
;;                     └─────────┘

(comment 
  (def api-key "<API-key>")
  (def router-response 
    [:map 
     [:action [:and {:description "Category of support request"}
               [:enum "refund" "tech-support" "general"]]]])

  (let [input "Hi I need to know what's the status of my request. When will I get my money back?"
        {:keys [action]} (i/instruct input
                                     router-response
                                     :api-key api-key
                                     :max-retries 0)]
    (case action
      "refund" (println "Routing to refund request")
      "tech-support" (println "Routing to tech support")
      "general" (println "Fetching general FAQ")
      nil))

  )
