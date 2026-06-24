package com.alfa.shakegroan.ui

import com.alfa.shakegroan.data.AppLanguage

data class AppStrings(
    val home: String,
    val settings: String,
    val upload: String,
    val profile: String,
    val sounds: String,
    val fall: String,
    val slap: String,
    val sensitivity: String,
    val volume: String,
    val language: String,
    val appLanguage: String,
    val mySounds: String,
    val library: String,
    val addSound: String,
    val extractFromVideo: String,
    val record: String,
    val uploadFile: String,
    val chooseVideo: String,
    val chooseAnotherVideo: String,
    val trimCurrentFragment: String,
    val save: String,
    val cancel: String,
    val name: String,
    val fragment: String,
    val buildingWaveform: String,
    val pressToRecord: String,
    val recording: String,
    val firstLaunchTitle: String,
    val firstLaunchText: String,
    val open: String,
    val later: String,
    val modeUnavailable: String,
    val modeOn: String,
    val modeOff: String,
    val guideTitle: String,
    val guideBody: String,
    val guideAndDescription: String,
    val installWidget: String,
) {
    companion object {
        fun forLanguage(language: AppLanguage): AppStrings = when (language) {
            AppLanguage.RU -> ru
            AppLanguage.ES -> base.copy(
                home = "Inicio",
                settings = "Ajustes",
                upload = "Carga",
                profile = "Perfil",
                sounds = "Sonidos",
                fall = "Caída",
                slap = "Palmada",
                sensitivity = "Sensibilidad",
                volume = "Volumen",
                language = "Idioma",
                appLanguage = "Idioma de la app",
                mySounds = "Mis sonidos",
                library = "Biblioteca",
                addSound = "+ añadir sonido",
                extractFromVideo = "Extraer de vídeo",
                record = "Grabar",
                uploadFile = "Subir",
                chooseVideo = "Elegir vídeo",
                chooseAnotherVideo = "Elegir otro vídeo",
                trimCurrentFragment = "Recortar fragmento actual",
                save = "Guardar",
                cancel = "Cancelar",
                name = "nombre",
                fragment = "Fragmento",
                buildingWaveform = "Creando onda...",
                pressToRecord = "Toca para grabar",
                recording = "Grabando %s",
                firstLaunchTitle = "¿Primer inicio?",
                firstLaunchText = "Abre la guía rápida: tiene consejos para ajustar la sensibilidad a tu teléfono.",
                open = "Abrir",
                later = "Luego",
                modeUnavailable = "modo no disponible",
                modeOn = "modo activado",
                modeOff = "modo desactivado",
                guideTitle = "Descripción e instrucciones",
                guideBody = """
                    1. Ajusta la «Sensibilidad». Los sensores pueden comportarse de forma distinta en cada teléfono. Si bajas la sensibilidad al mínimo, ese efecto se desactiva por completo.

                    2. Añade tus propios sonidos sin límites. Puedes grabar un sonido, subirlo desde el teléfono en casi cualquier formato o extraer audio de un vídeo, por ejemplo de un clip descargado o de una grabación de pantalla, y luego recortarlo.

                    3. Los sonidos se pueden editar y renombrar. Para hacerlo, desliza el sonido hacia la izquierda en el menú de selección.

                    4. Los sonidos subidos se pueden enviar a un amigo deslizando el sonido hacia la izquierda.

                    5. Puedes añadir un widget para activar y desactivar los efectos rápidamente.

                    6. Si algo no funciona o se comporta de forma extraña, escríbenos. Lo revisaremos e intentaremos arreglarlo. Esta es nuestra primera app y tu opinión es muy importante para nosotros :)

                    7. Si la app te sacó una sonrisa, por favor deja una reseña.
                """.trimIndent(),
                guideAndDescription = "Descripción e instrucciones",
                installWidget = "Instalar widget",
            )
            AppLanguage.IT -> base.copy(
                home = "Home",
                settings = "Impostazioni",
                upload = "Carica",
                profile = "Profilo",
                sounds = "Suoni",
                fall = "Caduta",
                slap = "Schiaffo",
                sensitivity = "Sensibilità",
                volume = "Volume",
                language = "Lingua",
                appLanguage = "Lingua app",
                mySounds = "I miei suoni",
                library = "Libreria",
                addSound = "+ aggiungi suono",
                extractFromVideo = "Estrai da video",
                record = "Registra",
                uploadFile = "Carica",
                chooseVideo = "Scegli video",
                chooseAnotherVideo = "Scegli un altro video",
                trimCurrentFragment = "Taglia frammento attuale",
                save = "Salva",
                cancel = "Annulla",
                name = "nome",
                fragment = "Frammento",
                buildingWaveform = "Creo onda...",
                pressToRecord = "Tocca per registrare",
                recording = "Registrazione %s",
                firstLaunchTitle = "Primo avvio?",
                firstLaunchText = "Apri la guida rapida: ci sono consigli per regolare la sensibilità sul tuo telefono.",
                open = "Apri",
                later = "Dopo",
                modeUnavailable = "modalità non disponibile",
                modeOn = "modalità attiva",
                modeOff = "modalità disattiva",
                guideTitle = "Descrizione e istruzioni",
                guideBody = """
                    1. Regola la «Sensibilità». I sensori possono funzionare in modo diverso su telefoni diversi. Se porti la sensibilità al minimo, l’effetto si disattiva completamente.

                    2. Aggiungi i tuoi suoni senza limiti. Puoi registrare un suono, caricarlo dal telefono in quasi qualsiasi formato oppure estrarre l’audio da un video, per esempio da una clip scaricata o da una registrazione dello schermo, e poi tagliarlo.

                    3. I suoni possono essere modificati e rinominati. Scorri il suono verso sinistra nel menu di selezione.

                    4. I suoni caricati possono essere inviati a un amico scorrendo il suono verso sinistra.

                    5. Puoi aggiungere un widget per attivare e disattivare rapidamente gli effetti.

                    6. Se qualcosa non funziona o si comporta in modo strano, scrivici. Controlleremo e proveremo a sistemarlo. Questa è la nostra prima app e il tuo feedback è davvero importante per noi :)

                    7. Se l’app ti ha fatto sorridere, lascia una recensione.
                """.trimIndent(),
                guideAndDescription = "Descrizione e istruzioni",
                installWidget = "Installa widget",
            )
            AppLanguage.PT_BR -> base.copy(
                home = "Início",
                settings = "Configurações",
                upload = "Enviar",
                profile = "Perfil",
                sounds = "Sons",
                fall = "Queda",
                slap = "Tapa",
                sensitivity = "Sensibilidade",
                volume = "Volume",
                language = "Idioma",
                appLanguage = "Idioma do app",
                mySounds = "Meus sons",
                library = "Biblioteca",
                addSound = "+ adicionar som",
                extractFromVideo = "Extrair do vídeo",
                record = "Gravar",
                uploadFile = "Enviar",
                chooseVideo = "Escolher vídeo",
                chooseAnotherVideo = "Escolher outro vídeo",
                trimCurrentFragment = "Cortar fragmento atual",
                save = "Salvar",
                cancel = "Cancelar",
                name = "nome",
                fragment = "Fragmento",
                buildingWaveform = "Criando onda...",
                pressToRecord = "Toque para gravar",
                recording = "Gravando %s",
                firstLaunchTitle = "Primeira vez?",
                firstLaunchText = "Abra o guia rápido: ele tem dicas para ajustar a sensibilidade ao seu telefone.",
                open = "Abrir",
                later = "Depois",
                modeUnavailable = "modo indisponível",
                modeOn = "modo ligado",
                modeOff = "modo desligado",
                guideTitle = "Descrição e instruções",
                guideBody = """
                    1. Ajuste a «Sensibilidade». Os sensores podem funcionar de formas diferentes em cada telefone. Se você reduzir a sensibilidade ao mínimo, esse efeito será totalmente desativado.

                    2. Adicione seus próprios sons sem limites. Você pode gravar um som, enviar do telefone em quase qualquer formato ou extrair áudio de um vídeo, por exemplo de um clipe baixado ou de uma gravação de tela, e depois cortar o trecho.

                    3. Os sons podem ser editados e renomeados. Para isso, deslize o som para a esquerda no menu de seleção.

                    4. Os sons enviados podem ser compartilhados com um amigo deslizando o som para a esquerda.

                    5. Você pode adicionar um widget para ligar e desligar os efeitos rapidamente.

                    6. Se algo não funcionar ou parecer estranho, escreva para nós. Vamos investigar e tentar corrigir. Este é nosso primeiro app, e seu feedback é muito importante para nós :)

                    7. Se o app fez você sorrir, deixe uma avaliação.
                """.trimIndent(),
                guideAndDescription = "Descrição e instruções",
                installWidget = "Instalar widget",
            )
            AppLanguage.DE -> base.copy(
                home = "Start",
                settings = "Einstellungen",
                upload = "Upload",
                profile = "Profil",
                sounds = "Sounds",
                fall = "Sturz",
                slap = "Klaps",
                sensitivity = "Empfindlichkeit",
                volume = "Lautstärke",
                language = "Sprache",
                appLanguage = "App-Sprache",
                mySounds = "Meine Sounds",
                library = "Bibliothek",
                addSound = "+ Sound hinzufügen",
                extractFromVideo = "Aus Video extrahieren",
                record = "Aufnehmen",
                uploadFile = "Hochladen",
                chooseVideo = "Video wählen",
                chooseAnotherVideo = "Anderes Video wählen",
                trimCurrentFragment = "Aktuelles Fragment schneiden",
                save = "Speichern",
                cancel = "Abbrechen",
                name = "name",
                fragment = "Fragment",
                buildingWaveform = "Wellenform wird erstellt...",
                pressToRecord = "Zum Aufnehmen tippen",
                recording = "Aufnahme %s",
                firstLaunchTitle = "Erster Start?",
                firstLaunchText = "Öffne die Kurzanleitung: dort findest du Tipps zur Empfindlichkeit deines Telefons.",
                open = "Öffnen",
                later = "Später",
                modeUnavailable = "Modus nicht verfügbar",
                modeOn = "Modus ein",
                modeOff = "Modus aus",
                guideTitle = "Beschreibung und Anleitung",
                guideBody = """
                    1. Passe die «Empfindlichkeit» an. Sensoren können sich je nach Telefon unterschiedlich verhalten. Wenn du die Empfindlichkeit ganz nach unten stellst, wird der Effekt vollständig deaktiviert.

                    2. Füge eigene Sounds ohne Limit hinzu. Du kannst einen Sound aufnehmen, ihn vom Telefon in fast jedem Format hochladen oder Audio aus einem Video extrahieren, zum Beispiel aus einem heruntergeladenen Clip oder einer Bildschirmaufnahme, und ihn danach zuschneiden.

                    3. Sounds können bearbeitet und umbenannt werden. Wische dazu im Sound-Auswahlmenü nach links.

                    4. Hochgeladene Sounds kannst du auch an einen Freund senden, indem du den Sound nach links wischst.

                    5. Du kannst ein Widget hinzufügen, um die Effekte schnell ein- und auszuschalten.

                    6. Wenn etwas nicht funktioniert oder sich falsch anfühlt, schreib uns bitte. Wir schauen es uns an und versuchen, es zu beheben. Das ist unsere erste App, und dein Feedback ist uns wirklich wichtig :)

                    7. Wenn dich die App zum Lächeln gebracht hat, hinterlasse bitte eine Bewertung.
                """.trimIndent(),
                guideAndDescription = "Beschreibung und Anleitung",
                installWidget = "Widget installieren",
            )
            AppLanguage.FR -> base.copy(
                home = "Accueil",
                settings = "Réglages",
                upload = "Import",
                profile = "Profil",
                sounds = "Sons",
                fall = "Chute",
                slap = "Claque",
                sensitivity = "Sensibilité",
                volume = "Volume",
                language = "Langue",
                appLanguage = "Langue de l’app",
                mySounds = "Mes sons",
                library = "Bibliothèque",
                addSound = "+ ajouter un son",
                extractFromVideo = "Extraire d’une vidéo",
                record = "Enregistrer",
                uploadFile = "Importer",
                chooseVideo = "Choisir une vidéo",
                chooseAnotherVideo = "Choisir une autre vidéo",
                trimCurrentFragment = "Couper le fragment actuel",
                save = "Enregistrer",
                cancel = "Annuler",
                name = "nom",
                fragment = "Fragment",
                buildingWaveform = "Création de l’onde...",
                pressToRecord = "Touchez pour enregistrer",
                recording = "Enregistrement %s",
                firstLaunchTitle = "Première ouverture ?",
                firstLaunchText = "Ouvre le guide rapide : il aide à régler la sensibilité selon ton téléphone.",
                open = "Ouvrir",
                later = "Plus tard",
                modeUnavailable = "mode indisponible",
                modeOn = "mode activé",
                modeOff = "mode désactivé",
                guideTitle = "Description et instructions",
                guideBody = """
                    1. Réglez la «Sensibilité». Les capteurs peuvent fonctionner différemment selon les téléphones. Si vous baissez la sensibilité au minimum, l’effet sera complètement désactivé.

                    2. Ajoutez vos propres sons sans limite. Vous pouvez enregistrer un son, l’importer depuis votre téléphone dans presque n’importe quel format ou extraire l’audio d’une vidéo, par exemple d’une vidéo téléchargée ou d’un enregistrement d’écran, puis le découper.

                    3. Les sons peuvent être modifiés et renommés. Pour cela, faites glisser le son vers la gauche dans le menu de sélection.

                    4. Les sons importés peuvent être envoyés à un ami en faisant aussi glisser le son vers la gauche.

                    5. Vous pouvez ajouter un widget pour activer et désactiver rapidement les effets.

                    6. Si quelque chose ne fonctionne pas ou semble étrange, écrivez-nous. Nous vérifierons et essaierons de corriger le problème. C’est notre première application, et votre retour compte beaucoup pour nous :)

                    7. Si l’application vous a fait sourire, laissez un avis.
                """.trimIndent(),
                guideAndDescription = "Description et instructions",
                installWidget = "Installer le widget",
            )
            AppLanguage.JA -> base.copy(
                home = "ホーム",
                settings = "設定",
                upload = "追加",
                profile = "プロフィール",
                sounds = "サウンド",
                fall = "落下",
                slap = "叩き",
                sensitivity = "感度",
                volume = "音量",
                language = "言語",
                appLanguage = "アプリの言語",
                mySounds = "マイサウンド",
                library = "ライブラリ",
                addSound = "+ サウンドを追加",
                extractFromVideo = "動画から抽出",
                record = "録音",
                uploadFile = "アップロード",
                chooseVideo = "動画を選択",
                chooseAnotherVideo = "別の動画を選択",
                trimCurrentFragment = "現在の部分をトリム",
                save = "保存",
                cancel = "キャンセル",
                name = "名前",
                fragment = "範囲",
                buildingWaveform = "波形を作成中...",
                pressToRecord = "タップして録音",
                recording = "録音 %s",
                firstLaunchTitle = "初回起動？",
                firstLaunchText = "クイックガイドで端末に合わせた感度調整を確認できます。",
                open = "開く",
                later = "あとで",
                modeUnavailable = "利用不可",
                modeOn = "オン",
                modeOff = "オフ",
                guideTitle = "説明と使い方",
                guideBody = """
                    1. 「感度」を調整してください。センサーの反応は端末によって異なることがあります。感度を最小まで下げると、その効果は完全にオフになります。

                    2. 自分のサウンドを制限なく追加できます。録音した音、端末内のほぼ任意の形式の音声、ダウンロードした動画や画面録画から抽出した音声を追加し、必要な部分だけ切り取れます。

                    3. サウンドは編集や名前変更ができます。サウンド選択画面で左にスワイプしてください。

                    4. 追加したサウンドは、左にスワイプして友だちに送ることもできます。

                    5. ウィジェットを追加すると、効果をすばやくオン・オフできます。

                    6. 何かが動かない、または期待どおりでない場合は、ぜひお知らせください。確認して修正できるようにします。これは私たちの最初のアプリなので、フィードバックはとても大切です :)

                    7. アプリで少しでも笑顔になれたら、レビューを残していただけるとうれしいです。
                """.trimIndent(),
                guideAndDescription = "説明と使い方",
                installWidget = "ウィジェットを追加",
            )
            AppLanguage.KO -> base.copy(
                home = "홈",
                settings = "설정",
                upload = "업로드",
                profile = "프로필",
                sounds = "사운드",
                fall = "낙하",
                slap = "찰싹",
                sensitivity = "감도",
                volume = "볼륨",
                language = "언어",
                appLanguage = "앱 언어",
                mySounds = "내 사운드",
                library = "라이브러리",
                addSound = "+ 사운드 추가",
                extractFromVideo = "동영상에서 추출",
                record = "녹음",
                uploadFile = "업로드",
                chooseVideo = "동영상 선택",
                chooseAnotherVideo = "다른 동영상 선택",
                trimCurrentFragment = "현재 구간 자르기",
                save = "저장",
                cancel = "취소",
                name = "이름",
                fragment = "구간",
                buildingWaveform = "파형 생성 중...",
                pressToRecord = "탭하여 녹음",
                recording = "녹음 %s",
                firstLaunchTitle = "처음 실행?",
                firstLaunchText = "빠른 안내에서 휴대폰에 맞는 감도 조절 팁을 확인하세요.",
                open = "열기",
                later = "나중에",
                modeUnavailable = "모드 사용 불가",
                modeOn = "모드 켜짐",
                modeOff = "모드 꺼짐",
                guideTitle = "설명 및 안내",
                guideBody = """
                    1. «감도»를 조절하세요. 휴대폰마다 센서 반응이 다를 수 있습니다. 감도를 최소로 낮추면 해당 효과가 완전히 꺼집니다.

                    2. 원하는 소리를 제한 없이 추가할 수 있습니다. 녹음기로 직접 녹음하거나, 휴대폰에서 거의 모든 형식의 파일을 가져오거나, 다운로드한 영상이나 화면 녹화에서 오디오를 추출한 뒤 필요한 부분만 자를 수 있습니다.

                    3. 소리는 편집하고 이름을 바꿀 수 있습니다. 소리 선택 메뉴에서 왼쪽으로 스와이프하세요.

                    4. 업로드한 소리는 왼쪽으로 스와이프해서 친구에게 보낼 수도 있습니다.

                    5. 위젯을 추가하면 효과를 빠르게 켜고 끌 수 있습니다.

                    6. 무언가 작동하지 않거나 이상하게 느껴지면 알려주세요. 확인하고 고치기 위해 노력하겠습니다. 이 앱은 저희의 첫 앱이라 여러분의 피드백이 정말 중요합니다 :)

                    7. 앱이 웃음을 줬다면 리뷰를 남겨주세요.
                """.trimIndent(),
                guideAndDescription = "설명 및 안내",
                installWidget = "위젯 설치",
            )
            AppLanguage.HI -> base.copy(
                home = "होम",
                settings = "सेटिंग्स",
                upload = "अपलोड",
                profile = "प्रोफ़ाइल",
                sounds = "ध्वनियाँ",
                fall = "गिरना",
                slap = "थप्पड़",
                sensitivity = "संवेदनशीलता",
                volume = "वॉल्यूम",
                language = "भाषा",
                appLanguage = "ऐप भाषा",
                mySounds = "मेरी ध्वनियाँ",
                library = "लाइब्रेरी",
                addSound = "+ ध्वनि जोड़ें",
                extractFromVideo = "वीडियो से निकालें",
                record = "रिकॉर्ड",
                uploadFile = "अपलोड",
                chooseVideo = "वीडियो चुनें",
                chooseAnotherVideo = "दूसरा वीडियो चुनें",
                trimCurrentFragment = "मौजूदा हिस्सा काटें",
                save = "सेव",
                cancel = "रद्द",
                name = "नाम",
                fragment = "हिस्सा",
                buildingWaveform = "वेवफॉर्म बना रहा है...",
                pressToRecord = "रिकॉर्ड करने के लिए टैप करें",
                recording = "रिकॉर्डिंग %s",
                firstLaunchTitle = "पहली बार?",
                firstLaunchText = "छोटी गाइड खोलें: इसमें आपके फोन के लिए संवेदनशीलता सेट करने की टिप्स हैं.",
                open = "खोलें",
                later = "बाद में",
                modeUnavailable = "मोड उपलब्ध नहीं",
                modeOn = "मोड चालू",
                modeOff = "मोड बंद",
                guideTitle = "विवरण और निर्देश",
                guideBody = """
                    1. «संवेदनशीलता» सेट करें। अलग-अलग फोन में सेंसर अलग तरह से काम कर सकते हैं। अगर संवेदनशीलता को न्यूनतम कर दें, तो वह प्रभाव पूरी तरह बंद हो जाएगा।

                    2. अपने ध्वनि प्रभाव बिना सीमा के जोड़ें। आप ध्वनि रिकॉर्ड कर सकते हैं, फोन से लगभग किसी भी फॉर्मेट में अपलोड कर सकते हैं, या किसी वीडियो से ऑडियो निकाल सकते हैं, जैसे डाउनलोड की गई क्लिप या स्क्रीन रिकॉर्डिंग, और फिर उसे काट सकते हैं।

                    3. ध्वनियों को संपादित और नाम बदला जा सकता है। इसके लिए ध्वनि चयन मेनू में ध्वनि को बाईं ओर स्वाइप करें।

                    4. अपलोड की गई ध्वनियाँ दोस्त को भेजी जा सकती हैं, ध्वनि को बाईं ओर स्वाइप करके।

                    5. प्रभावों को जल्दी चालू और बंद करने के लिए विजेट लगाया जा सकता है।

                    6. अगर कुछ काम नहीं करता या उम्मीद के मुताबिक नहीं लगता, तो हमें लिखें। हम जाँच करेंगे और ठीक करने की कोशिश करेंगे। यह हमारा पहला ऐप है, और आपका फीडबैक हमारे लिए बहुत महत्वपूर्ण है :)

                    7. अगर ऐप ने आपको मुस्कुराया, तो कृपया एक समीक्षा छोड़ें।
                """.trimIndent(),
                guideAndDescription = "विवरण और निर्देश",
                installWidget = "विजेट लगाएँ",
            )
            AppLanguage.ID -> base.copy(
                home = "Beranda",
                settings = "Pengaturan",
                upload = "Unggah",
                profile = "Profil",
                sounds = "Suara",
                fall = "Jatuh",
                slap = "Tamparan",
                sensitivity = "Sensitivitas",
                volume = "Volume",
                language = "Bahasa",
                appLanguage = "Bahasa aplikasi",
                mySounds = "Suara saya",
                library = "Pustaka",
                addSound = "+ tambah suara",
                extractFromVideo = "Ekstrak dari video",
                record = "Rekam",
                uploadFile = "Unggah",
                chooseVideo = "Pilih video",
                chooseAnotherVideo = "Pilih video lain",
                trimCurrentFragment = "Potong fragmen saat ini",
                save = "Simpan",
                cancel = "Batal",
                name = "nama",
                fragment = "Fragmen",
                buildingWaveform = "Membuat gelombang...",
                pressToRecord = "Ketuk untuk merekam",
                recording = "Merekam %s",
                firstLaunchTitle = "Pertama kali?",
                firstLaunchText = "Buka panduan singkat: ada tips untuk mengatur sensitivitas sesuai ponselmu.",
                open = "Buka",
                later = "Nanti",
                modeUnavailable = "mode tidak tersedia",
                modeOn = "mode aktif",
                modeOff = "mode nonaktif",
                guideTitle = "Deskripsi dan instruksi",
                guideBody = """
                    1. Atur «Sensitivitas». Sensor bisa bekerja berbeda di setiap ponsel. Jika sensitivitas diturunkan sampai minimum, efek tersebut akan mati sepenuhnya.

                    2. Tambahkan suara sendiri tanpa batas. Kamu bisa merekam suara, mengunggahnya dari ponsel dalam hampir semua format, atau mengekstrak audio dari video, misalnya dari klip yang diunduh atau rekaman layar, lalu memotong bagian yang dibutuhkan.

                    3. Suara bisa diedit dan diganti namanya. Geser suara ke kiri di menu pemilihan suara.

                    4. Suara yang diunggah juga bisa dikirim ke teman dengan menggeser suara ke kiri.

                    5. Kamu bisa menambahkan widget untuk menyalakan dan mematikan efek dengan cepat.

                    6. Jika ada yang tidak berfungsi atau terasa tidak sesuai, tulis kepada kami. Kami akan memeriksanya dan mencoba memperbaikinya. Ini aplikasi pertama kami, jadi masukanmu sangat penting :)

                    7. Jika aplikasi ini membuatmu tersenyum, mohon tinggalkan ulasan.
                """.trimIndent(),
                guideAndDescription = "Deskripsi dan instruksi",
                installWidget = "Pasang widget",
            )
            AppLanguage.EN_US -> base
        }

        private val base = AppStrings(
            home = "Home",
            settings = "Settings",
            upload = "Upload",
            profile = "Profile",
            sounds = "Sounds",
            fall = "Fall",
            slap = "Slap",
            sensitivity = "Sensitivity",
            volume = "Volume",
            language = "Language",
            appLanguage = "App language",
            mySounds = "My sounds",
            library = "Library",
            addSound = "+ add sound",
            extractFromVideo = "Extract from video",
            record = "Record",
            uploadFile = "Upload",
            chooseVideo = "Choose video",
            chooseAnotherVideo = "Choose another video",
            trimCurrentFragment = "Trim current fragment",
            save = "Save",
            cancel = "Cancel",
            name = "name",
            fragment = "Fragment",
            buildingWaveform = "Building waveform...",
            pressToRecord = "Tap to record",
            recording = "Recording %s",
            firstLaunchTitle = "First launch?",
            firstLaunchText = "Open the quick guide: it has tips for tuning sensitivity for your phone.",
            open = "Open",
            later = "Later",
            modeUnavailable = "mode unavailable",
            modeOn = "mode on",
            modeOff = "mode off",
            guideTitle = "Description and instructions",
            guideBody = """
                1. Set «Sensitivity». Sensors can behave differently on different phones. If you move sensitivity all the way down, that effect turns off completely.

                2. Add your own sounds without limits. You can record a sound, upload it from your phone in almost any format, or extract audio from a video, for example from a downloaded clip or a screen recording, and then trim it.

                3. Sounds can be edited and renamed. To do that, swipe a sound left in the sound picker.

                4. Uploaded sounds can be sent to a friend by swiping the sound left too.

                5. You can add a widget for quick effect on/off.

                6. If something does not work or feels wrong, please write to us. We will investigate and try to fix it. This is our first app, and your feedback really matters to us :)

                7. If the app made you smile, please leave a review.
            """.trimIndent(),
            guideAndDescription = "Description and instructions",
            installWidget = "Install widget",
        )

        private val ru = AppStrings(
            home = "Домой",
            settings = "Настройки",
            upload = "Загрузка",
            profile = "Профиль",
            sounds = "Звуки",
            fall = "Падение",
            slap = "Шлепок",
            sensitivity = "Чувствительность",
            volume = "Громкость",
            language = "Язык",
            appLanguage = "Язык приложения",
            mySounds = "Мои звуки",
            library = "Библиотека",
            addSound = "+ добавить звук",
            extractFromVideo = "Извлечь из видео",
            record = "Записать",
            uploadFile = "Загрузить",
            chooseVideo = "Выбрать видео",
            chooseAnotherVideo = "Выбрать другое видео",
            trimCurrentFragment = "Обрезать текущий фрагмент",
            save = "Сохранить",
            cancel = "Отмена",
            name = "название",
            fragment = "Фрагмент",
            buildingWaveform = "Строю волну...",
            pressToRecord = "Нажмите для записи",
            recording = "Запись %s",
            firstLaunchTitle = "Первый запуск?",
            firstLaunchText = "Открой короткую инструкцию: там есть советы, как подобрать чувствительность под свой телефон.",
            open = "Открыть",
            later = "Потом",
            modeUnavailable = "режим недоступен",
            modeOn = "режим включен",
            modeOff = "режим выключен",
            guideTitle = "Описание и инструкция",
            guideBody = """
                1. Настройте «Чувствительность». На разных телефонах датчики могут работать по-разному. Если уменьшить чувствительность до минимума, эффект полностью отключится.

                2. Добавляйте свои звуки без ограничений. Вы можете записать звук через диктофон, загрузить его с телефона в любом формате или извлечь звук из видео — например, из скачанного ролика или записи экрана — и отредактировать его.

                3. Звуки можно редактировать и переименовывать. Для этого свайпните звук влево в меню выбора звуков.

                4. Загруженные звуки можно отправить другу — также свайпнув звук влево.

                5. Можно установить виджет для быстрого включения и выключения эффектов.

                6. Если что-то не работает или работает не так, как ожидалось, пожалуйста, напишите нам. Мы обязательно разберёмся и постараемся всё исправить. Это наше первое приложение, и ваша обратная связь очень важна для нас :)

                7. Если вам понравилось приложение и оно вызвало у вас улыбку, пожалуйста, оставьте отзыв.
            """.trimIndent(),
            guideAndDescription = "Описание и инструкция",
            installWidget = "Установить виджет",
        )
    }
}
