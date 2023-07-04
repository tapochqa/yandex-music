(ns yandex-music.core
  (:require [clj-http.client :as client]
            [clj-commons.digest :as digest]
            [dynne.sampled-sound :as sampled-sound]

            [clojure.xml :as xml]
            [clojure.java.io :as io]
            [clojure.string :as string]))



(def f-id 99288656)


(def ua-string
  (str
    "Yandex-Music-API"))


(defn save-file! [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))


(defn log
  ([input]
     (prn input))
   
  ([input & inputs]
  (let [now (subs (str (java.time.LocalDateTime/now)) 11 19)]
    (if 
      (and (:host input) (:port input) (:session input) (:args input))
      (apply println (conj inputs now))
      (apply println (concat [now input] inputs))))))


(defn headers
  [{:keys [token]}]
  {"X-Yandex-Music-Client" "YandexMusicAndroid/23020251"
   "Authorization" (str "OAuth " token)})


(defn options
  [config]
  {:as :json
   :headers (headers config)
   :user-agent ua-string
   
   :decode-cookies false 
   :cookie-policy :none})


(defn y-hash
  [coll]
  (digest/md5
    (str "XGRlBW9FXlekgbPrRHuSiA"
         (subs (:path coll) 1)
         (:s coll))))
  

(defn track-download-info
  [config id]
  (->>
   (client/get
          (str "https://api.music.yandex.net/tracks/" id "/download-info")
          (options config)) 
      :body
      :result))


(defn track-result
  [config id]
  (->>
   (client/get
          (str "https://api.music.yandex.net/tracks/" id)
          (options config)) 
      :body
      :result
      first))



(defn track-link 
  [config id]
  (->>
    (track-download-info config id)
    (sort-by :bitrateInKbps)
    last
    :downloadInfoUrl))


(defn file-link
  [config id]
  (as-> (client/get (track-link config id)) r
      (:body r)
      (xml/parse (java.io.ByteArrayInputStream. (.getBytes r)))
      (:content r)
      (map (fn [a] {(:tag a) (-> a :content first)}) r)
      (reduce conj r)))


(defn dl-link
  [config id]
  (let
    [coll (file-link config id)]
    (str "https://"
      (:host coll)
      (str "/get-mp3/" (y-hash coll) "/" (:ts coll) (:path coll) "?track-id=" id))))


(defn album-result
  [config album-id]
  (->> (client/get (str "https://api.music.yandex.net/albums/" album-id "/with-tracks") (options config))
      :body :result))


(defn full-tracklist
  [config album-id]
  (->> (album-result config album-id)  
      :volumes
      (reduce conj)))


(defn tracklist
  [config album-id]
  (->> (album-result config album-id)  
      :volumes
      (reduce conj)
      (map :id)
      (map #(Integer/parseInt %))))


(defn explicit-list
  [config album-id]
  (as->   (config full-tracklist album-id) r
          (map (fn [item] {:explicit (some? (:contentWarning item))}) r)))


(defn cover-link
  [config album-id]
  (as-> (album-result config album-id) r
    (:coverUri r)
    (string/replace r #"%%" "1000x1000")
    (str "https://" r )))


(defn track-cover-link
  [config track-id]
  (as-> (track-result config track-id) r
    (:coverUri r)
    (string/replace r #"%%" "1000x1000")
    (str "https://" r )))



(defn dl!
  [config id path]
  (let [dir (str path "/yandex-" id)]
    (.mkdir (java.io.File. dir))
    (log "downloading" "a" "track from yandex music")
    (let [path (str dir "/" id)
          mp3-path (str path ".mp3")]
             (save-file!
               (dl-link config id)
               mp3-path)
             (log "✓")
      mp3-path)))


(comment
  
  (def CONFIG {:token (slurp "token")})
  
  (dl! CONFIG 6690056 "bounce")
  (cover-link CONFIG 23759549)
  
  (explicit-list CONFIG 13554547)
  
  (digest/md5 "privet")
  
  
  (spit "test.edn" (album-result CONFIG 19329554))

  (headers CONFIG)
  
  (options CONFIG)
  
  (track-cover-link CONFIG 115277207)
  
  (track-result CONFIG 115277207)
  
  (dl! CONFIG 115277207 "bounce")
  
  
  (save-file! (track-cover-link CONFIG 115277207) "bounce/cover.jpeg")
  
  (map conj [{:title "!" :artists [1 2 3]} {:title "?" }] '({:explicit true} {:explicit false}))

  
  )
