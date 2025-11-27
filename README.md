# Easter Bunny Egg Basket 🐰🥚

**Easter Bunny Egg Basket** is a small Android game built with Kotlin.  
You have **10 seconds** to tap the moving Easter egg as many times as possible — including rare, bonus golden eggs! This mini-game trains reaction speed and displays a polished summary card at the end of each round.

---

## 🎮 Gameplay

- ⏱ **10-second round** — the timer counts down until the game ends.
- 🥚 **Tap the moving egg** — it jumps around the screen after every tap.
- 🌈 **Different egg types** — regular, colorful, and **rare golden eggs**.
- ⭐ **Rare bonus eggs** are tracked separately and shown in the results.
- 🧮 **Score counter** displays how many eggs were collected this round.
- 🏆 **Top 5 Egg Hunters** are saved using `SharedPreferences`.
- 🪺 **End-of-game summary card** includes:
  - Total eggs this game  
  - Rare eggs collected  
  - A supportive, dynamic message based on performance

---

## 🔧 Technical Features

- **Kotlin**  
- **ConstraintLayout** for flexible and clean UI  
- Custom PNG assets (background, eggs, basket)  
- Polished animations:
  - Smooth hiding/showing of UI during gameplay
  - Animated summary card
- Prevents the egg from spawning under bottom buttons  
- **SharedPreferences** for saving high scores  
- Semi-transparent result card for better UX

---

## 📚 What I Learned

- Managing game state using **CountDownTimer**
- Building dynamic UI with **ConstraintLayout**
- Working with **custom resources** (drawables, strings, colors)
- Storing local game data with **SharedPreferences**
- Adding simple but effective **animations** for better user experience
- Structuring a small Android game loop in Kotlin

---

## 🚀 Future Enhancements (Ideas)

- Sound & particle effects  
- Multiple difficulty levels  
- Power-ups or special egg types  
- Leaderboard via Firebase  
- Character customization  

---

