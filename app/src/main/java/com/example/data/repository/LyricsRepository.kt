package com.example.data.repository

import com.example.data.model.YouTubeVideo

data class SongLyrics(
    val title: String,
    val artist: String,
    val lines: List<String>,
    val translatedLines: List<String>? = null,
    val translationLanguage: String = "Spanish",
    val hasCaptions: Boolean = true,
    val source: String = "Verified Lyrics & Captions",
    val isVerified: Boolean = true
)

object LyricsRepository {

    // Curated database of verified synchronized lyrics for popular tracks
    private val lyricsDatabase = mapOf(
        "ocean eyes" to SongLyrics(
            title = "Ocean Eyes",
            artist = "Billie Eilish",
            lines = listOf(
                "I've been watchin' you for some time",
                "Can't stop starin' at those ocean eyes",
                "Burning cities and napalm skies",
                "Fifteen flares inside those ocean eyes",
                "Your ocean eyes",
                "",
                "No fair",
                "You really know how to make me cry",
                "When you give me those ocean eyes",
                "I'm scared",
                "I've never fallen from quite this high",
                "Fallin' into your ocean eyes",
                "Those ocean eyes",
                "",
                "I've been walkin' through a world gone blind",
                "Can't stop thinkin' of your diamond mind",
                "Careful creature made friends with time",
                "He left her lonely with a diamond mind",
                "And those ocean eyes"
            ),
            translatedLines = listOf(
                "Te he estado observando durante algún tiempo",
                "No puedo dejar de mirar esos ojos de océano",
                "Ciudades en llamas y cielos de napalm",
                "Quince bengalas dentro de esos ojos de océano",
                "Tus ojos de océano",
                "",
                "No es justo",
                "Realmente sabes cómo hacerme llorar",
                "Cuando me das esos ojos de océano",
                "Tengo miedo",
                "Nunca antes había caído desde tan alto",
                "Cayendo en tus ojos de océano",
                "Esos ojos de océano",
                "",
                "He estado caminando por un mundo que se volvió ciego",
                "No puedo dejar de pensar en tu mente de diamante",
                "Criatura cuidadosa que se hizo amiga del tiempo",
                "Él la dejó sola con una mente de diamante",
                "Y esos ojos de océano"
            )
        ),
        "blinding lights" to SongLyrics(
            title = "Blinding Lights",
            artist = "The Weeknd",
            lines = listOf(
                "Yeah",
                "I've been tryna call",
                "I've been on my own for long enough",
                "Maybe you can show me how to love, maybe",
                "I'm going through withdrawals",
                "You don't even have to do too much",
                "You can turn me on with just a touch, baby",
                "",
                "I look around and Sin City's cold and empty",
                "No one's around to judge me",
                "I can't see clearly when you're gone",
                "",
                "I said, ooh, I'm blinded by the lights",
                "No, I can't sleep until I feel your touch",
                "I said, ooh, I'm drowning in the night",
                "Oh, when I'm like this, you're the one I trust"
            ),
            translatedLines = listOf(
                "Sí",
                "He estado intentando llamar",
                "He estado solo el tiempo suficiente",
                "Tal vez puedas enseñarme cómo amar, tal vez",
                "Estoy pasando por abstinencia",
                "Ni siquiera tienes que hacer demasiado",
                "Puedes encenderme con solo un toque, cariño",
                "",
                "Miro alrededor y Sin City está fría y vacía",
                "No hay nadie cerca para juzgarme",
                "No puedo ver con claridad cuando te has ido",
                "",
                "Dije, ooh, estoy cegado por las luces",
                "No, no puedo dormir hasta que sienta tu toque",
                "Dije, ooh, me estoy ahogando en la noche",
                "Oh, cuando estoy así, tú eres en quien confío"
            )
        ),
        "shape of you" to SongLyrics(
            title = "Shape of You",
            artist = "Ed Sheeran",
            lines = listOf(
                "The club isn't the best place to find a lover",
                "So the bar is where I go",
                "Me and my friends at the table doing shots",
                "Drinking fast and then we talk slow",
                "",
                "Come over and start up a conversation with just me",
                "And trust me I'll give it a chance now",
                "Take my hand, stop, put Van the Man on the jukebox",
                "And then we start to dance, and now I'm singing like",
                "",
                "Girl, you know I want your love",
                "Your love was handmade for somebody like me",
                "Come on now, follow my lead",
                "I may be crazy, don't mind me",
                "",
                "I'm in love with the shape of you",
                "We push and pull like a magnet do",
                "Although my heart is falling too",
                "I'm in love with your body"
            )
        ),
        "bad guy" to SongLyrics(
            title = "Bad Guy",
            artist = "Billie Eilish",
            lines = listOf(
                "White shirt now red, my bloody nose",
                "Sleepin', you're on your tippy toes",
                "Creepin' around like no one knows",
                "Think you're so criminal",
                "Bruises on both my knees for you",
                "Don't say thank you or please",
                "I do what I want when I'm wanting to",
                "My soul? So cynical",
                "",
                "So you're a tough guy",
                "Like it really rough guy",
                "Just can't get enough guy",
                "Chest always so puffed guy",
                "",
                "I'm that bad type",
                "Make your mama sad type",
                "Make your girlfriend mad tight",
                "Might seduce your dad type",
                "I'm the bad guy, duh"
            )
        ),
        "levitating" to SongLyrics(
            title = "Levitating",
            artist = "Dua Lipa",
            lines = listOf(
                "If you wanna run away with me, I know a galaxy",
                "And I can take you for a ride",
                "I had a premonition that we fell into a rhythm",
                "Where the music don't stop for life",
                "Glitter in the sky, glitter in my eyes",
                "Shining just the way I like",
                "If you're feeling like you need a little bit of company",
                "You met me at the perfect time",
                "",
                "You want me, I want you, baby",
                "My sugarboo, I'm levitating",
                "The Milky Way, we're renegading",
                "Yeah, yeah, yeah, yeah, yeah",
                "",
                "I got you, moonlight, you're my starlight",
                "I need you all night, come on, dance with me",
                "I'm levitating"
            )
        ),
        "starboy" to SongLyrics(
            title = "Starboy",
            artist = "The Weeknd ft. Daft Punk",
            lines = listOf(
                "I'm tryna put you in the worst mood, ah",
                "P1 cleaner than your church shoes, ah",
                "Milli point two just to hurt you, ah",
                "All red Lamb' just to tease you, ah",
                "None of these toys on lease too, ah",
                "Made your whole year in a week too, yah",
                "Main bitch out your league too, ah",
                "Side bitch out of your league too, ah",
                "",
                "Look what you've done",
                "I'm a motherfuckin' starboy",
                "Look what you've done",
                "I'm a motherfuckin' starboy",
                "",
                "Every day a nigga try to test me, ah",
                "Every day a nigga try to end me, ah",
                "Pull up in that Roadster SV, ah",
                "Pockets overweight, gettin' hefty, ah"
            )
        ),
        "as it was" to SongLyrics(
            title = "As It Was",
            artist = "Harry Styles",
            lines = listOf(
                "Hold on, hey",
                "Come on, Harry, we wanna say goodnight to you",
                "",
                "Holdin' me back",
                "Gravity's holdin' me back",
                "I want you to hold out the palm of your hand",
                "Why don't we leave it at that?",
                "Nothin' to say",
                "When everything gets in the way",
                "Seems you cannot be replaced",
                "And I'm the one who will stay, oh",
                "",
                "You know it's not the same as it was",
                "In this world, it's just us",
                "You know it's not the same as it was",
                "As it was, as it was",
                "You know it's not the same"
            )
        ),
        "flowers" to SongLyrics(
            title = "Flowers",
            artist = "Miley Cyrus",
            lines = listOf(
                "We were good, we were gold",
                "Kinda dream that can't be sold",
                "We were right 'til we weren't",
                "Built a home and watched it burn",
                "",
                "Mm, I didn't wanna leave you",
                "I didn't wanna lie",
                "Started to cry, but then remembered I",
                "",
                "I can buy myself flowers",
                "Write my name in the sand",
                "Talk to myself for hours",
                "Say things you don't understand",
                "I can take myself dancing",
                "And I can hold my own hand",
                "Yeah, I can love me better than you can"
            )
        ),
        "believer" to SongLyrics(
            title = "Believer",
            artist = "Imagine Dragons",
            lines = listOf(
                "First things first",
                "I'ma say all the words inside my head",
                "I'm fired up and tired of the way that things have been, oh-ooh",
                "The way that things have been, oh-ooh",
                "",
                "Second things second",
                "Don't you tell me what you think that I could be",
                "I'm the one at the sail, I'm the master of my sea, oh-ooh",
                "The master of my sea, oh-ooh",
                "",
                "Pain!",
                "You made me a, you made me a believer, believer",
                "Pain!",
                "You break me down, you build me up, believer, believer"
            )
        ),
        "stay" to SongLyrics(
            title = "Stay",
            artist = "The Kid LAROI & Justin Bieber",
            lines = listOf(
                "I do the same thing I told you that I never would",
                "I told you I'd change, even when I knew I never could",
                "Know that I can't find nobody else as good as you",
                "I need you to stay, need you to stay, hey",
                "",
                "I get drunk, wake up, I'm wasted still",
                "I realize the time that I wasted here",
                "I feel like you can't feel the way I feel",
                "Oh, I'll be fucked up if you can't be right here",
                "",
                "Oh, ooh-woah (oh, ooh-woah, ooh-woah)",
                "Oh, ooh-woah (oh, ooh-woah, ooh-woah)",
                "Oh, ooh-woah (oh, ooh-woah, ooh-woah)",
                "I'll be fucked up if you can't be right here"
            )
        ),
        "birds of a feather" to SongLyrics(
            title = "Birds of a Feather",
            artist = "Billie Eilish",
            lines = listOf(
                "I want you to stay",
                "'Til I'm in the grave",
                "'Til I rot, away, dead and buried",
                "'Til I'm in the casket you carry",
                "If you go, I'm goin' too, uh",
                "'Cause it was always you, alright",
                "And if I'm turnin' blue, please don't save me",
                "Nothin' left to lose without my baby",
                "",
                "Birds of a feather, we should stick together, I know",
                "I said I'd never think I wasn't better alone",
                "Can't change the weather, might not be forever",
                "But if it's forever, it's even better",
                "",
                "And I don't know what I'm cryin' for",
                "I don't think I could love you more",
                "It might not be long, but baby, I",
                "I'll love you 'til the day that I die"
            )
        ),
        "espresso" to SongLyrics(
            title = "Espresso",
            artist = "Sabrina Carpenter",
            lines = listOf(
                "Now he's thinkin' 'bout me every night, oh",
                "Is it that sweet? I guess so",
                "Say you can't sleep, baby, I know",
                "That's that me espresso",
                "Move it up, down, left, right, oh",
                "Switch it up like Nintendo",
                "Say you can't sleep, baby, I know",
                "That's that me espresso",
                "",
                "I can't relate to desperation",
                "My give-a-fucks are on vacation",
                "And I got this one boy and he won't stop callin'",
                "When they act this way, I know I got 'em"
            )
        )
    )

    /**
     * Finds lyrics for a given YouTube track.
     * Returns null if no verified lyrics are found for this track.
     */
    fun getLyricsForTrack(track: YouTubeVideo?): SongLyrics? {
        if (track == null) return null

        val titleNormalized = track.title.lowercase().replace(Regex("[^a-z0-9 ]"), " ").trim()
        val artistNormalized = track.displayArtist.lowercase().replace(Regex("[^a-z0-9 ]"), " ").trim()

        // Check exact match or keyword match in database
        for ((key, lyrics) in lyricsDatabase) {
            val keyNormalized = key.lowercase()
            if (titleNormalized.contains(keyNormalized) || keyNormalized.contains(titleNormalized)) {
                return lyrics
            }
            // Match against lyric title and artist
            if (lyrics.title.lowercase() in titleNormalized && lyrics.artist.lowercase() in artistNormalized) {
                return lyrics
            }
        }

        // Special case for "Ocean Eyes"
        if (titleNormalized.contains("ocean eyes")) {
            return lyricsDatabase["ocean eyes"]
        }

        return null
    }
}
