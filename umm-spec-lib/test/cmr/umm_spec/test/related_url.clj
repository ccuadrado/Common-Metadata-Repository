(ns cmr.umm-spec.test.related-url
  "Tests for cmr.umm-spec.related-url functions"
  (:require
   [clojure.test :refer :all]
   [cmr.common.util :refer [are3]]
   [cmr.umm-spec.dif-util :as dif-util]
   [cmr.umm-spec.models.umm-common-models :as cmn]
   [cmr.umm-spec.related-url :as related-url]
   [cmr.umm-spec.util :as su]))

(deftest related-url-key-value-mapping
  (is (= {:URLContentType "DistributionURL" :Type "GET DATA" :Subtype "Earthdata Search"}
         (get dif-util/dif-url-content-type->umm-url-types ["GET DATA" "Earthdata Search"] su/default-url-type))))

(deftest related-url-types
  (let [r1 (cmn/map->RelatedUrlType {:URLs ["cmr.earthdata.nasa.gov"]
                                     :URLContentType "DistributionURL"
                                     :Type "GET DATA"
                                     :MimeType "application/xml"})
        r2 (cmn/map->RelatedUrlType {:URLs ["cmr.earthdata.nasa.gov"]
                                     :URLContentType "VisualizationURL"
                                     :Type "GET RELATED VISUALIZATION"
                                     :MimeType "Text/rtf"})
        r3 (cmn/map->RelatedUrlType {:URLs ["cmr.earthdata.nasa.gov"]
                                     :URLContentType "PublicationURL"
                                     :Type "VIEW RELATED INFORMATION"
                                     :MimeType "application/json"})
        r4 (cmn/map->RelatedUrlType {:URLs ["cmr.earthdata.nasa.gov"]
                                     :URLContentType "DistributionURL"
                                     :Type "USE SERVICE API"
                                     :Subtype "OPENDAP DATA"
                                     :MimeType "Text/csv"})
        r5 (cmn/map->RelatedUrlType {:URLs ["cmr.earthdata.nasa.gov"]
                                     :URLContentType "VisualizationURL"
                                     :Type "GET RELATED VISUALIZATION"
                                     :Subtype "MAP"
                                     :MimeType "Text/csv"})
        urls [r1 r2 r3 r4 r5]]

    (testing "Downloadable URLs"
      (is (= [r1] (related-url/downloadable-urls urls))))

    (testing "Browse URLs"
      (is (= [r2 r5] (related-url/browse-urls urls))))

    (testing "Resource URLs"
      (is (= [r3 r4] (related-url/resource-urls urls))))

    (testing "Atom link types"
      (is (= ["data" "browse" "documentation" "metadata" "browse"]
             (map :link-type (related-url/atom-links urls)))))))
