(ns cmr.system-int-test.search.collection-keyword-search-test
  "Integration test for CMR collection search by keyword terms"
  (:require [clojure.test :refer :all]
            [cmr.common.util :as u :refer [are2]]
            [cmr.system-int-test.utils.ingest-util :as ingest]
            [cmr.system-int-test.utils.search-util :as search]
            [cmr.system-int-test.utils.index-util :as index]
            [cmr.system-int-test.data2.collection :as dc]
            [cmr.system-int-test.data2.core :as d]
            [cmr.search.data.keywords-to-elastic :as k2e]))

(use-fixtures :each (ingest/reset-fixture {"provguid1" "PROV1" "provguid2" "PROV2" "provguid3" "PROV3"}))

(deftest search-by-keywords
  (let [short-name-boost (k2e/get-boost nil :short-name)
        project-boost (k2e/get-boost nil :project)
        platform-boost (k2e/get-boost nil :platform)
        instrument-boost (k2e/get-boost nil :instrument)
        sensor-boost (k2e/get-boost nil :sensor)
        concept-id-boost (k2e/get-boost nil :concept-id)
        provider-boost (k2e/get-boost nil :provider)
        entry-title-boost (k2e/get-boost nil :entry-title)
        science-keywords-boost (k2e/get-boost nil :science-keywords)
        psa1 (dc/psa {:name "alpha" :data-type :string :value "ab"})
        psa2 (dc/psa {:name "bravo" :data-type :string :value "bf"})
        psa3 (dc/psa {:name "charlie" :data-type :string :value "foo"})
        psa4 (dc/psa {:name "case" :data-type :string :value "up"})
        psa5 (dc/psa {:name "novalue" :data-type :string :description "description"})
        p1 (dc/platform
            {:short-name "platform_SnB"
             :long-name "platform_Ln B"
             :instruments
             [(dc/instrument {:short-name "isnA" :long-name "ilnA" :technique "itechniqueA"
                              :sensors [(dc/sensor {:short-name "ssnA" :long-name "slnA"})
                                        (dc/sensor {:short-name "ssnD" :long-name "slnD"
                                                    :technique "techniqueD"})]})]})
        p2 (dc/platform
            {:short-name "platform_SnA spoonA"
             :long-name "platform_LnA"
             :characteristics [(dc/characteristic {:name "char1" :description "char1desc"})
                               (dc/characteristic {:name "char2" :description "char2desc"})]
             :instruments
             [(dc/instrument {:short-name "isnB" :long-name "ilnB" :technique "itechniqueB"
                              :sensors [(dc/sensor {:short-name "ssnB" :long-name "slnB"
                                                    :technique "techniqueB"})
                                        (dc/sensor {:short-name "ssnC" :long-name "slnC"
                                                    :technique "techniqueC"})]})]})
        p3 (dc/platform {:short-name "spoonA"})
        p4 (dc/platform {:short-name "SMAP"
                         :instruments [(dc/instrument {:short-name "SMAP L-BAND RADIOMETER"})]})
        p5 (dc/platform {:short-name "fo&nA"})
        p6 (dc/platform {:short-name "spo~nA"})
        pboost (dc/platform {:short-name "boost"})
        pr1 (dc/projects "project-short-name")
        sk1 (dc/science-keyword {:category "Cat1"
                                 :topic "Topic1"
                                 :term "Term1"
                                 :variable-level-1 "Level1-1"
                                 :variable-level-2 "Level1-2"
                                 :variable-level-3 "Level1-3"
                                 :detailed-variable "SUPER DETAILED!"})
        sk2 (dc/science-keyword {:category "Hurricane"
                                 :topic "Laser spoonA"
                                 :term "Extreme"
                                 :variable-level-1 "Level2-1"
                                 :variable-level-2 "Level2-2"
                                 :variable-level-3 "Level2-3"})
        sk3 (dc/science-keyword {:category "Cat2"
                                 :topic "Topic1"
                                 :term "Term1"
                                 :variable-level-1 "Level3-1"
                                 :variable-level-2 "Level3-2"
                                 :variable-level-3 "Level3-3"
                                 :detailed-variable "S@PER"})
        skboost (dc/science-keyword {:category "boost"
                                     :topic "boost"
                                     :term "boost"
                                     :variable-level-1 "boost"
                                     :variable-level-2 "boost"
                                     :variable-level-3 "boost"
                                     :detailed-variable "boost"})
        tdcs1 (dc/two-d "XYZ")
        coll1 (d/ingest "PROV1" (dc/collection
                                 {:entry-title "coll1" :version-description "VersionDescription"}))
        coll2 (d/ingest "PROV1" (dc/collection
                                 {:entry-title "coll2" :short-name "ABC!XYZ" :version-id "V001"}))
        coll3 (d/ingest "PROV1" (dc/collection {:entry-title "coll3" :collection-data-type "OTHER"}))
        coll4 (d/ingest "PROV2" (dc/collection {:entry-title "coll4" :collection-data-type "OTHER"}))
        coll5 (d/ingest "PROV2" (dc/collection {:entry-title "coll5" :long-name "ABC" :short-name "Space!Laser"}))
        coll6 (d/ingest "PROV2" (dc/collection {:entry-title "coll6" :organizations [(dc/org :archive-center "Some&Place")]}))
        coll7 (d/ingest "PROV2" (dc/collection {:entry-title "coll7" :version-id "Laser"}))
        coll8 (d/ingest "PROV2" (dc/collection {:entry-title "coll8" :processing-level-id "PDQ123"}))

        coll9 (d/ingest "PROV2" (dc/collection {:entry-title "coll09" :science-keywords [sk1 sk2]}))


        coll10 (d/ingest "PROV2" (dc/collection {:entry-title "coll10"
                                                 :spatial-keywords ["in out"]
                                                 :science-keywords [sk3]}))
        coll11 (d/ingest "PROV2" (dc/collection {:entry-title "coll11" :platforms [p2 p3 p5 p6]
                                                 :product-specific-attributes [psa5]}))
        coll12 (d/ingest "PROV2" (dc/collection {:entry-title "coll12" :product-specific-attributes [psa1 psa2 psa3 psa4]}))
        coll13 (d/ingest "PROV2" (dc/collection {:entry-title "coll13" :two-d-coordinate-systems [tdcs1]}))
        coll14 (d/ingest "PROV2" (dc/collection {:entry-title "coll14" :long-name "spoonA laser"}))
        coll15 (d/ingest "PROV2" (dc/collection {:entry-title "coll15" :processing-level-id "plid1"
                                                 :collection-data-type "SCIENCE_QUALITY" :platforms [p1]
                                                 :summary "summary" :temporal-keywords ["tk1" "tk2"]}))
        coll16 (d/ingest "PROV2" (dc/collection-dif {:short-name "entryid4"}) {:format :dif})
        coll17 (d/ingest "PROV2" (dc/collection {:associated-difs ["DIF-1" "DIF-2"]}))
        coll18 (d/ingest "PROV2" (dc/collection {:short-name "SNFoobar"}))
        coll19 (d/ingest "PROV2" (dc/collection {:long-name "LNFoobar"}))
        coll20 (d/ingest "PROV2" (dc/collection {:projects pr1}))
        coll21 (d/ingest "PROV2" (dc/collection {:entry-title "coll21" :long-name "ABC!"}))
        coll22 (d/ingest "PROV2" (dc/collection {:collection-data-type "NEAR_REAL_TIME"}))
        coll23 (d/ingest "PROV1" (dc/collection {:entry-title "coll23" :long-name "\"Quoted\" collection"}))
        coll24 (d/ingest "PROV2" (dc/collection {:entry-title "coll24" :short-name "coll24" :platforms [p4]}))
        coll-boost (d/ingest "PROV2" (dc/collection {:entry-title "boost"
                                                     :short-name "boost"
                                                     :platforms [pboost]
                                                     :science-keywords [skboost]}))]

    (index/wait-until-indexed)

    (testing "search by keywords."
      (are [keyword-str items]
        (let [parameter-refs (search/find-refs :collection {:keyword keyword-str})
              json-refs (search/find-refs-with-json-query :collection {} {:keyword keyword-str})
              parameter-matches? (d/refs-match? items parameter-refs)
              json-matches? (d/refs-match? items json-refs)]
          (when-not parameter-matches?
            (println "Parameter search failed")
            (println "Expected:" (map :entry-title items))
            (println "Actual:" (map :name (:refs parameter-refs))))
          (when-not json-matches?
            (println "JSON Query search failed")
            (println "Expected:" (map :entry-title items))
            (println "Actual:" (map :name (:refs json-refs))))
          (and parameter-matches? json-matches?))

        "ABC" [coll2 coll5 coll21]
        "place" [coll6]
        "Laser" [coll5 coll7 coll9 coll14]
        "ABC space" [coll5]
        "BLAH" []
        "abc!" [coll21]

        ;; Checking specific fields

        ;; provider
        "PROV1" [coll1 coll2 coll3 coll23]

        ;; entry title
        "coll1" [coll1]

        ;; entry id
        "entryid4" [coll16]

        ;; short name
        "XYZ" [coll2 coll13]

        ;; long name
        "ABC" [coll5 coll2 coll21]

        ;; version id
        "V001" [coll2]

        ;; version description
        "VersionDescription" [coll1]

        ;; processing level id
        "plid1" [coll15]

        ;; collection data type
        "SCIENCE_QUALITY" [coll15]

        ;; collection data type aliases for NEAR_REAL_TIME
        "NEAR_REAL_TIME" [coll22]
        "NRT" [coll22]
        "near_real_time" [coll22]
        "nrt" [coll22]
        "near-real-time" [coll22]
        "near real-time" [coll22]
        "near-real time" [coll22]

        ;; summary
        "summary" [coll15]

        ;; temporal keywords
        "tk1" [coll15]

        ;; spatial keywords
        "in" [coll10]

        ;; associated difs
        "dif-1" [coll17]

        ;; two d coord
        "xyz" [coll2 coll13]

        ;; archive center
        "some" [coll6]

        ;; attributes
        ;; - name
        "charlie" [coll12]
        ;; - description
        "Generated" [coll12]
        ;; description with no value - see CMR-1129
        "description" [coll11]

        ;; Platforms
        ;; - short name
        "platform_SnA" [coll11]
        ;; - long name (from metadata - not from KMS)
        "platform_ln" [coll15]
        ;; - long name (from KMS - not from the metadata)
        "Soil Moisture Active and Passive Observatory" [coll24]
        ;; - characteristic name
        "char1" [coll11]
        "char2" [coll11]
        "char1 char2" [coll11]
        ;; - chracteristic description
        "char1desc" [coll11]
        "char2desc" [coll11]
        "char1desc char2desc" [coll11]

        ;; Instruments
        ;; - short name
        "isnA" [coll15]
        ;; - long name (from metadata - not from KMS)
        "ilnB" [coll11]
        ;; - long name (from KMS - not from metadata)
        "SMAP L-Band Radiometer" [coll24]
        ;; - technique
        "itechniqueB" [coll11]
        "itechniqueA" [coll15]

        ;; Sensors
        ;; - short name
        "ssnA" [coll15]
        ;; - long name
        "slnB" [coll11]
        ;; - technique
        "techniqueB" [coll11]
        "techniqueD" [coll15]
        "techniqueB techniqueC" [coll11]

        ;; Science keywords
        ;; - category
        "Cat1" [coll9]
        ;; - topic
        "Topic1" [coll9 coll10]
        ;; - term
        "Term1" [coll9 coll10]
        ;; - variable-levels
        "Level2-1" [coll9]
        "Level2-2" [coll9]
        "Level2-3" [coll9]
        ;; - detailed-variable
        "SUPER" [coll9]

        ;; Special characters are escaped before sending to Elastic
        "ABC~ZYX" []
        "ABC~" []
        "spo~nA" [coll11]
        "fo&nA" [coll11]
        "A.+&.+C" []
        "S@PER" [coll10]

        ;; search by keywords using wildcard *
        "A*C" [coll2 coll5 coll21]
        "XY*" [coll2 coll13]
        "*aser" [coll5 coll7 coll9 coll14]
        "p*ce" [coll6]
        "NEA*REA*IME" [coll22]
        "nea*rea*ime" [coll22]
        "\"Quoted*" [coll23]

        ;; search by keywords using wildcard ?
        "A?C" [coll2 coll5 coll21]
        "XY?" [coll2 coll13]
        "?aser" [coll5 coll7 coll9 coll14]
        "p*ace" [coll6]
        "NEAR?REAL?TIME" [coll22]
        "near?real?time" [coll22]))

    (testing "Default boosts on fields"
      (are2 [params scores] (= (map #(/ % 2.0) scores)
                               (map :score (:refs (search/find-refs :collection
                                                                    params))))

        ;; Q: Are there any in facets that are not in keywords? They may not be scored appropriately.

        ;; TODO test cases
        ;; - every field that is part of facets
        ;; - case sensitive search
        ;; - spaces and special characters in search term
        ;; - pattern search (how does that apply to boosting)

        "short-name"
        {:keyword "SNFoobar"} [short-name-boost]
        "long-name"
        {:keyword "LNFoobar"} [short-name-boost]

        "project short-name as keyword"
        {:keyword (:short-name (first pr1))} [project-boost]
        "project short-name as parameter"
        {:project (:short-name (first pr1))} [project-boost]
        "project long-name"
        {:keyword (:long-name (first pr1))} [project-boost]

        "platform short-name as keyword"
        {:keyword (:short-name p1)} [platform-boost]
        "platform short-name as parameter"
        {:platform (:short-name p1)} [platform-boost]
        "platform long-name (from metadata)"
        {:keyword (:long-name p1)} [platform-boost]
        "platform long-name (from KMS)"
        {:keyword "Soil Moisture Active and Passive Observatory"} [platform-boost]

        "instrument short-name as keyword"
        {:keyword (:short-name (first (:instruments p1)))} [instrument-boost]
        "instrument short-name as parameter"
        {:instrument (:short-name (first (:instruments p1)))} [instrument-boost]
        "instrument long-name (from metadata)"
        {:keyword (:long-name (first (:instruments p1)))} [instrument-boost]
        "instrument long-name (from KMS)"
        {:keyword "L-Band Radiometer"} [instrument-boost]

        "sensor short-name as keyword"
        {:keyword (:short-name (first (:sensors (first (:instruments p1)))))} [sensor-boost]
        "sensor short-name as parameter"
        {:sensor (:short-name (first (:sensors (first (:instruments p1)))))} [sensor-boost]
        "sensor long-name"
        {:keyword (:long-name (first (:sensors (first (:instruments p1)))))} [sensor-boost]

        "temporal-keywords"
        {:keyword "tk1"} [(k2e/get-boost nil :temporal-keyword)]

        "spatial-keyword"
        {:keyword "in out"} [(k2e/get-boost nil :spatial-keyword)]

        "science-keywords as keyword"
        {:keyword (:category sk1)} [science-keywords-boost]
        "science-keywords category as parameter"
        {:science-keywords {:0 {:category (:category sk1)}}} [science-keywords-boost]

        "science-keywords topic as parameter"
        {:science-keywords {:0 {:topic (:topic sk1)}}} [science-keywords-boost science-keywords-boost]
        "science-keywords term as parameter"
        {:science-keywords {:0 {:term (:term sk1)}}} [science-keywords-boost science-keywords-boost]
        "science-keywords variable-level-1 as parameter"
        {:science-keywords {:0 {:variable-level-1 (:variable-level-1 sk1)}}} [science-keywords-boost]
        "science-keywords variable-level-2 as parameter"
        {:science-keywords {:0 {:variable-level-2 (:variable-level-2 sk1)}}} [science-keywords-boost]
        "science-keywords variable-level-3 as parameter"
        {:science-keywords {:0 {:variable-level-3 (:variable-level-3 sk1)}}} [science-keywords-boost]
        "science-keywords any as parameter"
        {:science-keywords {:0 {:any (:category sk1)}}} [science-keywords-boost]

        "version-id"
        {:keyword "V001"} [(k2e/get-boost nil :version-id)]

        "entry-title"
        {:keyword "coll5"} [(k2e/get-boost nil :entry-title)]

        "provider-id"
        {:keyword "PROV1"} [provider-boost provider-boost provider-boost provider-boost]))
    (testing "Specified boosts on fields"
      (are [params scores] (= (map #(/ % 2.0) scores)
                              (map :score (:refs (search/find-refs :collection params))))
        ;; short-name
        {:keyword "SNFoobar":boosts {:short-name 2.0}} [2.0]

        ;; project short-name
        {:keyword (:short-name (first pr1)) :boosts {:project 3.0}} [3.0]

        ;; platform short-name
        {:keyword (:short-name p1) :boosts {:platform 4.0}} [4.0]

        ;; instrument short-name
        {:keyword (:short-name (first (:instruments p1))) :boosts {:instrument 5.0}} [5.0]

        ;; sensor short-name
        {:keyword (:short-name (first (:sensors (first (:instruments p1))))) :boosts {:sensor 6.0}} [6.0]

        ;; temporal-keywords
        {:keyword "tk1" :boosts {:temporal-keyword 7.0}} [7.0]

        ;; spatial-keyword
        {:keyword "in out" :boosts {:spatial-keyword 8.0}} [8.0]

        ;; science-keywords
        {:keyword (:category sk1) :boosts {:science-keywords 9.0}} [9.0]

        ;; version-id
        {:keyword "V001" :boosts {:version-id 10.0}} [10.0]

        ;; provider-id
        {:keyword "PROV1" :boosts {:provider 10.0}} [10.0 10.0 10.0 10.0]

        ;; entry-title
        {:keyword "coll5" :boosts {:entry-title 10.0}} [10.0]

        ;; mixed boosts
        {:keyword "Laser spoonA" :boosts {:short-name 10.0 :science-keywords 11.0}} [11.0 10.0]

        ;; no defaults
        {:keyword (:category sk1) :boosts {:include-defaults false}} [1.0]

        ;; matches all fields, do not include defaults
        {:keyword "boost" :boosts {:short-name 5.0 :include-defaults false}} [5.0]

        ;; matches all fields, use defaults, but override short-name boost
        {:keyword "boost" :boosts {:short-name 5.0 :include-defaults true}}
        [(* 5.0 entry-title-boost platform-boost science-keywords-boost)]))

    (testing "Setting boosts without keyword search is an error"
      (let [resp (search/find-refs :collection {:provider "PROV1"
                                                :page_size 5
                                                :short_name "SNFoobar"
                                                :boosts {:short-name 2.0}})
            {:keys [status errors]} resp]
        (is (= 400 status))
        (is (= "Relevance boosting is only supported for keyword queries" (first errors)))))

    (testing "Boosting on invalid field"
      (let [resp (search/find-refs :collection {:provider "PROV1"
                                                :page_size 5
                                                :keyword "Laser"
                                                :boosts {:foo 2.0}})
            {:keys [status errors]} resp]
        (is (= 400 status))
        (is (= "Cannot set relevance boost on field [foo]." (first errors)))))

    (testing "Boosting with non-numeric values is an error."
      (let [resp (search/find-refs :collection {:provider "PROV1"
                                                :page_size 5
                                                :keyword "Laser"
                                                :boosts {:short-name "foo"}})
            {:keys [status errors]} resp]
        (is (= 400 status))
        (is (= "Relevance boost value [foo] for field [short_name] is not a number." (first errors)))))

    (testing "sorted search by keywords."
      (are [keyword-str items]
        (let [refs (search/find-refs :collection {:keyword keyword-str})
              matches? (d/refs-match-order? items refs)]
          (when-not matches?
            (println "Expected:" (map :entry-title items))
            (println "Actual:" (map :name (:refs refs))))
          matches?)
        "Laser spoonA" [coll14 coll9]
        "La?er spoonA" [coll14 coll9]
        "L*er spo*A" [coll14 coll9]
        "L?s* s?o*A" [coll14 coll9]))

    (testing "sorted search by keywords JSON query."
      (are [keyword-str items]
        (let [refs (search/find-refs-with-json-query :collection {} {:keyword keyword-str})
              matches? (d/refs-match-order? items refs)]
          (when-not matches?
            (println "Expected:" (map :entry-title items))
            (println "Actual:" (map :name (:refs refs))))
          matches?)
        "Laser spoonA" [coll14 coll9]))

    (testing "sorted search by keywords with sort keys."
      (are [keyword-str sort-key items]
        (let [refs (search/find-refs :collection {:keyword keyword-str :sort-key sort-key})
              matches? (d/refs-match-order? items refs)]
          (when-not matches?
            (println "Expected:" (map :entry-title items))
            (println "Actual:" (map :name (:refs refs))))
          matches?)
        "Laser" "-entry-title" [coll7 coll5 coll14 coll9]
        "Laser" "score" [coll14 coll5 coll7 coll9]
        "Laser" "+score" [coll5 coll7 coll9 coll14]
        "Laser" "-score" [coll14 coll5 coll7 coll9]))

    (testing "parameter search by keywords returns score"
      (let [refs (search/find-refs :collection {:keyword "Laser"})]
        (is (every? :score (:refs refs)))))

    (testing "JSON keywords search returns score"
      (let [refs (search/find-refs-with-json-query :collection {} {:keyword "Laser"})]
        (is (every? :score (:refs refs)))))

    (testing "search by multiple keywords returns an error."
      (let [resp (search/find-refs :collection {:provider "PROV1"
                                                :page_size 5
                                                :keyword ["Laser" "spoon"]})
            {:keys [status errors]} resp]
        (is (= 400 status))
        (is (= "Parameter [keyword] must have a single value." (first errors)))))

    (testing "JSON negated keyword search does not return score"
      (let [refs (search/find-refs-with-json-query :collection {} {:not {:keyword "Laser"}})]
        (is (not-any? :score (:refs refs)))))))

;; This tests that when searching by relevancy that if the score is the same short name ascending is used for
;; sorting the results and then if short name is the same version is used for sorting the results
(deftest search-by-keywords-relevancy-sorting-includes-short-name-and-version
  (let [coll1 (d/ingest "PROV1" (dc/collection
                                 {:entry-title "MODIS/Terra Aerosol 5-Min L2 Swath 10km V5.1",
                                  :short-name "MOD04_L2",
                                  :version-id "5.1"}))
        coll2 (d/ingest "PROV1" (dc/collection
                                 {:entry-title "MODIS/Aqua Aerosol 5-Min L2 Swath 10km V5.1",
                                  :short-name "MYD04_L2",
                                  :version-id "5.1"}))
        coll3 (d/ingest "PROV1" (dc/collection
                                 {:entry-title "MODIS/Aqua Aerosol 5-Min L2 Swath 10km V006",
                                  :short-name "MYD04_L2",
                                  :version-id "6"}))
        coll4 (d/ingest "PROV1" (dc/collection
                                 {:entry-title "MODIS/Terra Aerosol 5-Min L2 Swath 10km V006",
                                  :short-name "MOD04_L2",
                                  :version-id "6"}))
        coll5 (d/ingest "PROV1" (dc/collection
                                 {:entry-title "MODIS/Aqua Total Precipitable Water Aerosol 5-Min L2 Swath 1km and 5km V5.1",
                                  :short-name "MYD05_L2",
                                  :version-id "5.1"}))
        coll6 (d/ingest "PROV1" (dc/collection
                                 {:entry-title "MODIS/Terra Total Precipitable Water Aerosol 5-Min L2 Swath 1km and 5km V5.1",
                                  :short-name "MOD05_L2",
                                  :version-id "5.1"}))
        coll7 (d/ingest "PROV1" (dc/collection
                                 {:entry-title "MODIS/Aqua Total Precipitable Water Aerosol 5-Min L2 Swath 1km and 5km V006",
                                  :short-name "MYD05_L2",
                                  :version-id "6"}))
        coll8 (d/ingest "PROV1" (dc/collection
                                 {:entry-title "MODIS/Terra Total Precipitable Water Aerosol 5-Min L2 Swath 1km and 5km V006",
                                  :short-name "MOD05_L2",
                                  :version-id "6"}))
        coll9 (d/ingest "PROV1" (dc/collection
                                 {:entry-title "MODIS Aerosol Other",
                                  :projects [(dc/project "MODIS" "ignored")]
                                  :platforms [(dc/platform {:short-name "MODIS"})]
                                  :short-name "Other",
                                  :version-id "1"}))]
    (index/wait-until-indexed)
    (let [refs (search/find-refs :collection {:keyword "modis aerosol"})
          expected-order [coll9 ;; higher score
                          ;; The scores of the rest of them are identical
                          coll4 ;; MOD04_L2 6
                          coll1 ;; MOD04_L2 5.1
                          coll8 ;; MOD05_L2 6
                          coll6 ;; MOD05_L2 5.1
                          coll3 ;; MYD04_L2 6
                          coll2 ;; MYD04_L2 5.1
                          coll7 ;; MYD05_L2 6
                          coll5] ;; MYD05_L2 5.1
          matched? (d/refs-match-order?
                    expected-order
                    refs)]
      (when-not matched?
        (println "Actual order: " (pr-str (map :id (:refs refs)))))
      (is matched?))))


(deftest search-by-keywords-with-special-chars
  ;; needed for special charatcter tests
  (let [coll-data [["coll00" "dummy && ||"]
                   ["coll01" "begin!end"]
                   ["coll02" "begin@end"]
                   ["coll03" "begin#end"]
                   ["coll04" "begin$end"]
                   ["coll05" "begin%end"]
                   ["coll06" "begin^end"]
                   ["coll07" "begin&end"]
                   ["coll08" "begin(end"]
                   ["coll09" "begin)end"]
                   ["coll10" "begin-end"]
                   ["coll11" "begin=end"]
                   ["coll12" "begin_end"]
                   ["coll13" "begin+end"]
                   ["coll14" "begin{end"]
                   ["coll15" "begin}end"]
                   ["coll16" "begin[end"]
                   ["coll17" "begin]end"]
                   ["coll18" "begin|end"]
                   ["coll19" "begin\\end"]
                   ["coll20" "begin;end"]
                   ["coll21" "begin'end"]
                   ["coll22" "begin.end"]
                   ["coll23" "begin,end"]
                   ["coll24" "begin/end"]
                   ["coll25" "begin:end"]
                   ["coll26" "begin\"end"]
                   ["coll27" "begin<end"]
                   ["coll28" "begin>end"]
                   ["coll29" "begin?end"]
                   ["coll30" "begin`end"]
                   ["coll31" "begin~end"]
                   ["coll32" "modis foobar"]
                   ["coll33" "bleep blop blorp"]
                   ["coll34" "abcdefghijklmnop"]
                   ["coll35" "foo modis bar"]
                   ["coll36" "akdi modis/terra dke"]
                   ["coll37" "akdi modis-terra dke"]
                   ["coll38" "Dataset with foobar"]
                   ["coll39" "foo54"]
                   ["coll40" "foo67"]
                   ["coll41" "moding"]
                   ["coll42" "outmoded"]
                   ["coll43" "outmodising"]
                   ["coll44" "out-modis-ed"]
                   ["coll45" "carbon*oxygen"]
                   ["coll46" "Dataset no withword"]
                   ["coll47" "begin&&end"]
                   ["coll48" "choco and taco"]
                   ["coll49" "choco or taco"]
                   ["coll50" "begin*end"]]
        colls (doall (for [[coll summary] coll-data]
                       (d/ingest "PROV3" (dc/collection {:entry-title coll :summary summary}))))]
    (index/wait-until-indexed)
    (are [keyword-str indexes]
         (let [refs (search/find-refs :collection {:keyword keyword-str :page_size 51})
               items (map (partial nth colls) indexes)
               matches? (d/refs-match? items refs)]
           (when-not matches?
             (println "Expected:" (map :entry-title items))
             (println "Actual:" (map :name (:refs refs))))
           matches?)
         "begin!end" [1]
         "begin\\end" [19]
         "begin<end" [27]
         "begin\\?end" [29]
         "begin~end" [31]
         "begin\\*end" [50]
         "begin" [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 47 50]
         "end" [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 47 50]
         "&&" [0]
         "||" [0]
         "AND" [48]
         "OR" [49])))
