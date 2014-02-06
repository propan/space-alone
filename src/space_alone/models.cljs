(ns space-alone.models
  (:require [space-alone.constants :as C]
            [space-alone.utils :as u]))

(defrecord Asteroid [x y vX vY energy size type])

(defrecord Bullet [x y vX vY energy])

(defrecord Ship [x y vX vY thrust rotation rotate accelerate shoot next-shoot])

(defrecord GameScreen [asteroids bullets ship next-asteroid lives score])

(defrecord WelcomeScreen [])

(defn random-speed
  [size]
  (let [direction (if (< (Math/random) 0.5) -1 1)]
    (case size
      :large  (* direction (u/random-float 0.01 0.3))
      :medium (* direction (u/random-float 0.1  0.5))
      :small  (* direction (u/random-float 0.2  0.8)))))

(defn asteroid
  [x y size]
  ;; TODO: improve speed generation to avoid initial blinking on the edge
  (Asteroid. x y (random-speed size) (random-speed size) (size C/ASTEROID_POWERS) size (u/random-int 1 4)))

(defn bullet
  [x y rotation]
  (let [vX (* C/BULLET_SPEED (Math/sin (* rotation (- C/RAD_FACTOR))))
        vY (* C/BULLET_SPEED (Math/cos (* rotation (- C/RAD_FACTOR))))]
    (Bullet. (- x vX) (- y vY) vX vY C/BULLET_ENERGY)))

(defn ship
  [x y]
  (Ship. x y 0 0 0 0 :none false false 0))

(defn game-screen
  []
  (GameScreen. [] []
               (ship (/ C/SCREEN_WIDTH 2)
                     (/ C/SCREEN_HEIGHT 2))
               (u/random-int C/MIN_TIME_BEFORE_ASTEROID
                             C/MAX_TIME_BEFORE_ASTEROID)
               3 0))
(defn welcome-screen
  []
  (WelcomeScreen.))
