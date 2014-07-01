(ns cmr.umm.dif.collection.spatial-coverage
  "Provide functions to parse and generate DIF spatial-coverage info, it is mapped to an Extended_Metadata element."
  (:require [cmr.umm.collection :as c]
            [cmr.umm.dif.collection.extended-metadata :as em]
            [cmr.spatial.mbr :as m]
            [clojure.data.xml :as x]
            [cmr.common.xml :as cx]
            [camel-snake-kebab :as csk])
  (:import cmr.spatial.mbr.Mbr))

(def SPATIAL_COVERAGE_EXTERNAL_META_NAME
  "GranuleSpatialRepresentation")

(defn- extract-granule-spatial-representation
  [xml-struct]
  ;; DIF: Extended_Metadata.Name=GranuleSpatialRepresention
  (when-let [ems (em/xml-elem->extended-metadatas xml-struct false)]
    (let [rep (filter #(= SPATIAL_COVERAGE_EXTERNAL_META_NAME (:name %)) ems)]
      (when (seq rep)
        (csk/->kebab-case-keyword (:value (first rep)))))))

(defn- spatial-coverage-elem->br
  "Converts a DIF Spatial_Coverage element into a bounding rectangle"
  [elem]
  (let [w (cx/double-at-path elem [:Westernmost_Longitude])
        n (cx/double-at-path elem [:Northernmost_Latitude])
        e (cx/double-at-path elem [:Easternmost_Longitude])
        s (cx/double-at-path elem [:Southernmost_Latitude])]
    (m/mbr w n e s)))

(defn xml-elem->SpatialCoverage
  "Returns a UMM SpatialCoverage from a parsed Collection XML structure"
  [xml-struct]
  (let [gsr (extract-granule-spatial-representation xml-struct)
        spatial-coverage-elems (cx/elements-at-path xml-struct [:Spatial_Coverage])]
    (when (or gsr (seq spatial-coverage-elems))
      (c/map->SpatialCoverage
        {:granule-spatial-representation gsr
         :spatial-representation (when (seq spatial-coverage-elems) :cartesian)
         :geometries (seq (map spatial-coverage-elem->br spatial-coverage-elems))}))))

(defn generate-spatial-coverage-extended-metadata
  "Generates the extended metadata for spatial coverage which contains the granule spatial representation."
  [spatial-coverage]
  (when spatial-coverage
    (let [extended-metadata {:name SPATIAL_COVERAGE_EXTERNAL_META_NAME
                             :value (csk/->SNAKE_CASE_STRING (:granule-spatial-representation spatial-coverage))}]
      (em/generate-extended-metadatas [extended-metadata] false))))

(defn generate-spatial-coverage
  "Generates the Spatial_Coverage elements"
  [spatial-coverage]
  (let [brs (filter #(= Mbr (type %)) (:geometries spatial-coverage))]
    (map (fn [br]
           (x/element :Spatial_Coverage {}
                      (x/element :Southernmost_Latitude {} (:south br))
                      (x/element :Northernmost_Latitude {} (:north br))
                      (x/element :Westernmost_Longitude {} (:west br))
                      (x/element :Easternmost_Longitude {} (:east br))))
         brs)))