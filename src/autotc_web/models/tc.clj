(ns autotc-web.models.tc
  (:require [clj-teamcity-api.api :as tc]
            [autotc-web.log :as log]
            [autotc-web.models.exception :as exception])
  (:import clj_teamcity_api.net.TeamCityServer)
  (:import clj_teamcity_api.net.Credentials))

(defn- tag? [tag-name]
  #(= tag-name (:tag %)))

(defn- empty-list-if-nil [x]
  (if (nil? x) '() x))

(defn- project-by-name [projects name]
  (->> projects
       :content
       (filter (fn [project]
                 (= name (get-in project [:attrs :name]))))
       first
       :attrs))

(defn- project-build-type-ids [project]
  (let [build-types-tag (->> project
                             :content
                             (filter (tag? :buildTypes))
                             first)
        build-type-ids (->> build-types-tag
                            :content
                            (map #(get-in % [:attrs :id])))]
    build-type-ids))

(defn- last-builds-view [last-builds]
  (->> last-builds
       :content
       (map :attrs)))

(defn- build-view [build]
  (let [info (:content build)
        test-occurences (->> info
                             (filter (tag? :testOccurrences))
                             first
                             :attrs)
        changes (->> info
                     (filter (tag? :lastChanges))
                     first
                     :content
                     (map :attrs))
        status-text (->> info
                         (filter (tag? :statusText))
                         first
                         :content
                         first)
        agent (->> info
                   (filter (tag? :agent))
                   first
                   :attrs)]
    {:test-occurences (empty-list-if-nil test-occurences)
     :changes (empty-list-if-nil changes)
     :status-text status-text
     :agent agent}))

;; (defn vcs-root [server auth root]
;;   (let [properties (->> root
;;                         :content
;;                         (filter (tag? :properties))
;;                         first
;;                         :content
;;                         (filter (tag? :property))
;;                         (map (fn [tag]
;;                                (let [attrs (:attrs tag)]
;;                                  (vector (keyword (:name attrs))
;;                                          (:value attrs)))))
;;                         (apply concat)
;;                         (apply hash-map))]

;;     properties))

(defn project-info [host port project-name user pass]
  (let [server (TeamCityServer. host port)
        credentials (Credentials. user pass)
        project-id (:id (project-by-name (tc/projects server credentials) project-name))
        project (tc/project server credentials project-id)
        build-type-ids (project-build-type-ids project)
        build-types (doall (pmap (fn [build-type-id]
                                   (try (let [last-build (->> build-type-id
                                                              (tc/last-builds server credentials)
                                                              last-builds-view
                                                              first)
                                              build-type (->> build-type-id
                                                              (tc/build-type server credentials)
                                                              :attrs)
                                              last-build-details (->> last-build
                                                                      :id
                                                                      (tc/build server credentials)
                                                                      build-view)]
                                          {:last-build last-build
                                           :build-type build-type
                                           :last-build-details last-build-details})
                                        (catch Exception e
                                          (log/error e (format "cant get info for build type id=[%s]" build-type-id))
                                          {:error (exception/pretty-print-exception e)})))
                                 build-type-ids))]
    {:build-types build-types}))

(defn trigger-build [host port user pass build-type-id]
  (let [server (TeamCityServer. host port)
        credentials (Credentials. user pass)]
    (tc/trigger-build server credentials build-type-id)))

(defn cancel-build [host port user pass build-type-id]
  (let [server (TeamCityServer. host port)
        credentials (Credentials. user pass)
        running-build-id (->> (tc/running-build server credentials build-type-id)
                              last-builds-view
                              first
                              :id)]
    (if (not (nil? running-build-id))
      (tc/cancel-build server credentials running-build-id))))

(defn reboot-agent [host port user pass build-type-id]
  (let [server (TeamCityServer. host port)
        credentials (Credentials. user pass)
        ;; TODO: think to replace this heuristics to agent requirement parameters
        last-build-id (->> (tc/last-builds server credentials build-type-id)
                           last-builds-view
                           first
                           :id)
        last-build (tc/build server credentials last-build-id)
        agent-id (->> last-build
                      :content
                      (filter (tag? :agent))
                      first
                      :attrs
                      :id)]
    (tc/reboot-agent server credentials agent-id)))
