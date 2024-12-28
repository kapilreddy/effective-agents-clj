(ns effective-agents-clj.workflows.prompt-chaining
  (:require [instructor-clj.core :as i]
            [clojure.string :as cs]))

;;  In                                                  Out
;; [O]-> [LLM1] -> [Gate] --Pass--> [LLM2] -> [LLM3] -> [O]
;;                    |
;;                  Fail
;;                    |
;;                    v
;;                 [Exit]
(comment 
  
  (def api-key "<API-key>")

  (def Output
    [:map [:output :string]])

  (let [product "Apple"
        llm-1 (i/instruct (format "Create compelling marketing copy for %s. 
    The copy should:
    - Be between 50-100 words
    - Include a clear value proposition
    - Have a call to action
    - Use persuasive language
    Output the copy only, no additional text."
                                  product)
                          Output
                          :api-key api-key
                          :max-retries 2)]
    (def k* llm-1)
    ;; Gate
    (when (< (-> llm-1
                 :output
                 (cs/split #"\W")
                 count)
             100)
      (let [llm-2 (i/instruct (format "Translate following text in Hindi\n %s" (:output llm-1))
                              Output
                              :api-key api-key
                              :max-retries 0)
            llm-3 (i/instruct (format "Write a 15 word title in Hindi this text\n%s" (:output llm-2))
                              Output
                              :api-key api-key
                              :max-retries 0)]
        (< (-> llm-3
               :output
               (cs/split #"\W")
               count)
           15))))

  )
