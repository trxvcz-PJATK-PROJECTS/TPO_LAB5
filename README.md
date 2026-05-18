# SIMPLECHAT_VT

Wielowątkowy serwer i klient czatu zaimplementowany w języku Java. W odróżnieniu od poprzedniej wersji, ten projekt **wymaga użycia wirtualnych wątków (Virtual Threads)** do obsługi żądań klientów. 

**🔴 UWAGA KRYTYCZNA: W żadnym z kodów NIE WOLNO stosować kanałów NIO (`java.nio.channels`). Należy użyć standardowego wejścia/wyjścia opartego na gniazdach (`java.net.Socket`, `java.net.ServerSocket`) w połączeniu z wirtualnymi wątkami.**

## 📌 Opis Projektu

Celem projektu jest stworzenie serwera czatu, który:
* Obsługuje logowanie klientów (identyfikacja po ID, bez hasła).
* Przyjmuje wiadomości od poszczególnych klientów i rozsyła je do wszystkich **zalogowanych** użytkowników.
* Obsługuje wylogowanie klientów.
* W przypadku zatrzymania serwera rozsyła informację o zakończeniu czatu (`ChatServer: chat closed`) do wszystkich klientów, którzy w tym momencie są jeszcze zalogowani.
* Gromadzi pełny log aktywności w pamięci wewnętrznej serwera (z czasem z dokładnością do nanosekund).

## 🏗️ Architektura Systemu

### 1. `ChatServer`
Klasa realizująca logikę serwera. **Obsługa żądań klientów musi odbywać się w wirtualnych wątkach.**
* **Konstruktor:** `public ChatServer(int port)`
* `public void startServer()` – Uruchamia serwer w odrębnym wątku, wypisując na konsoli: `Server started`.
* `public void stopServer()` – Zatrzymuje serwer oraz jego wątek, wypisując na konsoli: `Server stopped`. Powoduje rozesłanie wiadomości `ChatServer: chat closed` do aktualnie zalogowanych klientów.
* `public String getServerLog()` – Zwraca pełny log serwera w wymaganym formacie.

### 2. `ChatClient`
Klasa realizująca logikę klienta opartą na standardowych gniazdach wejścia-wyjścia.
* **Konstruktor:** `public ChatClient(String host, int port, String id)` (gdzie `id` to identyfikator klienta).
* `public void login()` – Loguje klienta do czatu.
* `public void logout()` – Wylogowuje klienta.
* `public void send(String req)` – Wysyła do serwera żądanie `req` (tekst wiadomości, logowanie, wylogowanie - własny protokół komunikacji).
* `public String getChatView()` – Zwraca pełną historię wiadomości, którą otrzymał dany klient od momentu logowania.

### 3. `ChatClientTask`
Klasa umożliwiająca uruchamianie klientów w odrębnych wątkach poprzez `ExecutorService`.
* **Tworzenie:** `public static ChatClientTask create(ChatClient c, List<String> msgs, int wait)`
* **Działanie w wątku:** 1. Wywołuje `c.login()`.
    2. Wysyła kolejne wiadomości z listy `msgs` przy pomocy `c.send()`.
    3. Wywołuje `c.logout()`.
    *Uwaga:* Po każdym żądaniu (login, send, logout) wątek jest wstrzymywany na `wait` milisekund. Jeśli `wait == 0`, wątek klienta nie jest wstrzymywany.

### 4. `MainChatVT` *(plik niemodyfikowalny)*
Główna klasa testująca, która odczytuje konfigurację z pliku `ChatTestVT.yaml` umieszczonego w katalogu `{user.home}` i uruchamia symulację.

## ⚙️ Konfiguracja (Plik YAML)

Klasa `MainChatVT` wczytuje plik konfiguracyjny `ChatTestVT.yaml`.

**Przykładowy format pliku:**
```yaml
portAndDelays: [ 7799, 0, 50]
Asia: [30, 3]
Adam: [30, 3]
Sara: [30, 3]
```
**Znaczenie parametrów:**
* `portAndDelays: [port, startTaskDelay, stopServerDelay]`
  * `port` - numer portu serwera.
  * `startTaskDelay` - opóźnienie w ms przed startem kolejnego zadania klienta.
  * `stopServerDelay` - opóźnienie w ms przed zatrzymaniem serwera.
* `id_klienta: [wait, liczba_wiadomości]`
  * `wait` - opóźnienie po wysłaniu każdego żądania (w ms).
  * `liczba_wiadomości` - ilość automatycznie generowanych wiadomości (w formacie `msg_1`, `msg_2`, itd.).

## 📝 Formatowanie Danych i Logów

Ścisłe przestrzeganie formy wydruku jest **obowiązkowe** i wpływa na punktację.

### Konsola podczas pracy
* Start serwera: `Server started`
* Zatrzymanie serwera: `Server stopped`

### Akcje na serwerze (Log & Rozsyłanie)
* **Logowanie:** Serwer rozsyła: `id logged in`.
* **Wylogowanie:** Serwer rozsyła: `id logged out`.
* **Wiadomość:** Serwer rozsyła: `id: msg`.
* **Zatrzymanie serwera:** Serwer dodaje do logu i rozsyła do zalogowanych klientów: `ChatServer: chat closed`.

### Widok czatu (Chat View) dla klienta `id`
Musi zaczynać się od nagłówka wygenerowanego przez `MainChatVT`:
```text
=== id chat view
```
Klient ma przechowywać tylko treść otrzymanych wiadomości, **bez znaczników czasu**. Otrzymuje tylko te komunikaty, które serwer rozesłał podczas jego obecności na czacie (pomiędzy jego logowaniem a wylogowaniem/zamknięciem serwera).

### Log Serwera
Musi zaczynać się od nagłówka wygenerowanego przez `MainChatVT`:
```text
=== Server log
```
Każdy wpis w logu musi zawierać dokładny czas zgłoszenia w formacie `HH:MM:SS.nnn`, gdzie **nnn to nanosekundy**, np.:
```text
17:34:29.600410400 Adam logged in
17:34:29.611412600 Sara: msg_1
17:34:29.798997800 ChatServer: chat closed
```

## ⚠️ Ważne uwagi

* Kolejność wierszy, dokładne czasy czy kompletność komunikatów w logach będą się różnić w zależności od ustawionych opóźnień (np. przy `stopServerDelay = 0` serwer może zostać zamknięty zanim klienci zdążą wysłać wszystkie wiadomości – co jest poprawnym zachowaniem symulacji pokazanym w Przykładzie 3).
* Należy zadbać o poprawną synchronizację struktur danych wewnątrz `ChatServer` – z serwerem łączy się równolegle wielu klientów obsługiwanych przez wirtualne wątki.
