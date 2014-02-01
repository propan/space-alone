(ns space-alone.models
  (:require [space-alone.constants :as C]
            [space-alone.utils :as u]))

(defrecord Asteroid [x y vX vY energy size])

(defrecord Bullet [x y energy rotation])

(defrecord Ship [x y vX vY thrust rotation rotate accelerate shoot next-shoot])

(defn random-speed
  [size]
  (case size
    :large  (u/random-float 0.01 0.3)
    :medium (u/random-float 0.1  0.5)
    :small  (u/random-float 0.2  0.8)))

(defn asteroid
  [x y size]
  (Asteroid. x y (random-speed size) (random-speed size) (size C/ASTEROID_POWERS) size))

(defn bullet
  [x y rotation]
  (Bullet. x y C/BULLET_ENERGY rotation))

(defn ship
  [x y]
  (Ship. x y 0 0 0 0 :none false false 0))
