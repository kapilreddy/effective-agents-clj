(ns effective-agents-clj.workflows.parallelization
  (:require [instructor-clj.core :as i]
            [clojure.string :as cs]))

;;             ┌─────────┐
;;        ┌───>│ LLM 1   │─┐
;;        │    └─────────┘ │
;; ┌──┐   │    ┌─────────┐ │   ┌────────────┐    ┌───┐
;; │In│───┼───>│ LLM 2   │─┼──>│Aggregator  │───>│Out│
;; └──┘   │    └─────────┘ │   └────────────┘    └───┘
;;        │    ┌─────────┐ │
;;        └───>│ LLM 3   │─┘
;;             └─────────┘


(comment 
  (def api-key "<API-KEY>"))


(def code-quality-review-response
  [:map 
   [:issues-found [:or :map :string]]
   [:code-smells [:or :map :string]]
   [:maintainability-score :int]
   [:improvement-suggestions [:or :map :string]]])

(def security-review-response 
  [:map 
   [:has-vulnerabilities :boolean]
   [:vulnerabilities :string]
   [:risk-level :int]
   [:recommendations :string]])

(def code-with-security-issues
  "
    def authenticate_user(username, password):
       conn = sqlite3.connect('users.db')
       cursor = conn.cursor()
    
       # Security issue: SQL injection vulnerability
       query = f\"SELECT * FROM users WHERE username='{username}' AND password='{password}'\"
       cursor.execute(query)
    
       user = cursor.fetchone()
       return user is not None")

(def code-with-quality-issues 
  "
    def do_stuff(x):  # Poor naming
       # No docstring
       y = []  # Poor variable naming
       for i in range(len(x)):  # Anti-pattern
          if x[i] > 0:
             y.append(x[i] * 2)
          else:
             y.append(0)
       return y")

(def code-with-both-cases
  "
    def process_user_data(d):  # Poor naming
       # No input validation
       # No error handling
       conn = sqlite3.connect('app.db')
       c = conn.cursor()
    
       # Security issue: SQL injection
       q = f\"UPDATE users SET data='{d}' WHERE id=1\"
       c.execute(q)
       conn.commit()")

(def security-review-prompt 
  "Perform a security review of the following code. 
     Focus exclusively on security aspects:
        1. SQL injection vulnerabilities
        2. Authentication/authorization issues
        3. Data exposure risks
        4. Input validation
        5. Secure communication
        6. Secure storage

        Code to review:
        %s")

(def code-quality-prompt 
  "Perform a code quality review of the following code. Focus exclusively on:
        1. Code organization and structure
        2. Naming conventions
        3. Function/method design
        4. Error handling
        5. Comments and documentation
        6. Performance considerations
        7. Testing possibilities

        Code to review:
        %s")

(defn review-code
  [x]
  (let [model "gpt-4o"
        reviewers [{:prompt code-quality-prompt
                    :reviewer :code-quality
                    :response code-quality-review-response} 
                   {:prompt security-review-prompt
                    :reviewer :security
                    :response security-review-response}]
        output-xs (pmap (fn [{:keys [prompt response reviewer]}]
                          {:reviewer reviewer
                           :outupt (i/instruct (format prompt x)
                                               response
                                               :model model
                                               :api-key api-key
                                               :max-retries 0)})
                        reviewers)]
    output-xs))

;; Sectioning
(comment 
  
  (def output-xs* (review-code code-with-security-issues))

  )


;; Voting

(def vote-response
  [:map 
   [:vote-type [:enum "safe" "unsafe" "unsure"]]
   [:confidence :double]
   [:reasoning :string]])

(def conservative-voter
  "Evaluate the following content with a conservative approach.
     Flag any content that could be potentially harmful, even if mild.
        
     Content: %s")

(def lenient-voter 
  "Evaluate the following content with a lenient approach.
      Only flag content that is clearly and substantially harmful.
       
      Content: %s")

(def context-aware-voter
  "Evaluate the following content considering context and audience.
       Consider:
       1. Intended audience
       2. Platform context
       3. Cultural context
       4. Educational value
       
       Content: %s")

(def intent-focused-voter
  "Evaluate the following content considering context and audience.
       Consider:
       1. Intended audience
       2. Platform context
       3. Cultural context
       4. Educational value
       
       Content: %s")

(def expert-domain-voter
  "Evaluate the following content as a domain expert.
        Consider:
        1. Technical accuracy
        2. Professional standards
        3. Industry best practices
        4. Potential consequences
        
        Content: %s")

(def content 
  "How to create a computer virus:
     1. Disable antivirus
     2. Create malicious payload
     3. Add self-replication code
     4. Distribute to victims")

(defn review-content
  [x]
  (let [model "gpt-4o"
        voters [[:conservative conservative-voter]
                [:lenient lenient-voter]
                [:context-aware context-aware-voter]
                [:intent-focused intent-focused-voter]
                [:expert-domain expert-domain-voter]]
        votes (pmap (fn [[voter voter-prompt]]
                      [voter (i/instruct (format voter-prompt x)
                                         vote-response
                                         :model model
                                         :api-key api-key
                                         :max-retries 0)])
                    voters)
        majority-vote (-> (sort-by (comp - second) 
                                   (frequencies (map (comp :vote-type second)
                                                     votes)))
                          first
                          first)]
    majority-vote))


(comment 

  (def output* (review-content content))

  )
