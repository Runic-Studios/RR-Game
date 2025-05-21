package com.runicrealms.game.common

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.util.ChatPaginator

const val LINE_LENGTH = 28
const val CENTER_PX = 154

fun Player.sendError(message: String) {
    sendMessage(Component.text(message).color(TextColor.color(0xbf0202)))
}

fun String.colorFormat(altChar: Char = '&'): TextComponent =
    LegacyComponentSerializer.legacy(altChar).deserialize(this)

fun TextComponent.toLegacy(altChar: Char = '&'): String =
    LegacyComponentSerializer.legacy(altChar).serialize(this)

object TextIcons {
    const val HEALTH_ICON: String = "❤"
    const val EMPTY_GEM_ICON: String = "◇"
}

fun String.breakLines(lineLength: Int = LINE_LENGTH): List<String> {
    return listOf(*ChatPaginator.wordWrap(this, lineLength))
}

fun sendCenteredMessage(player: Player, message: String) {
    val centeredMessage = centeredMessage(player, message)
    player.sendMessage(centeredMessage + message)
}

fun centeredMessage(player: Player, msg: String): String {
    val message = msg.colorFormat().toLegacy()
    if (message == "") player.sendMessage("")

    var messagePxSize = 0
    var previousCode = false
    var isBold = false

    for (c in message.toCharArray()) {
        if (c == LegacyComponentSerializer.SECTION_CHAR) {
            previousCode = true
        } else if (previousCode) {
            previousCode = false
            isBold = c == 'l' || c == 'L'
        } else {
            val dFI = DefaultFontEnum.getDefaultFontInfo(c)
            messagePxSize += if (isBold) dFI.boldLength else dFI.length
            messagePxSize++
        }
    }

    val halvedMessageSize = messagePxSize / 2
    val toCompensate = CENTER_PX - halvedMessageSize
    val spaceLength = DefaultFontEnum.SPACE.length + 1
    var compensated = 0
    val sb = StringBuilder()
    while (compensated < toCompensate) {
        sb.append(" ")
        compensated += spaceLength
    }
    return sb.toString()
}

enum class DefaultFontEnum(val character: Char, val length: Int) {
    A('A', 5),
    a('a', 5),
    B('B', 5),
    b('b', 5),
    C('C', 5),
    c('c', 5),
    D('D', 5),
    d('d', 5),
    E('E', 5),
    e('e', 5),
    F('F', 5),
    f('f', 4),
    G('G', 5),
    g('g', 5),
    H('H', 5),
    h('h', 5),
    I('I', 3),
    i('i', 1),
    J('J', 5),
    j('j', 5),
    K('K', 5),
    k('k', 4),
    L('L', 5),
    l('l', 1),
    M('M', 5),
    m('m', 5),
    N('N', 5),
    n('n', 5),
    O('O', 5),
    o('o', 5),
    P('P', 5),
    p('p', 5),
    Q('Q', 5),
    q('q', 5),
    R('R', 5),
    r('r', 5),
    S('S', 5),
    s('s', 5),
    T('T', 5),
    t('t', 4),
    U('U', 5),
    u('u', 5),
    V('V', 5),
    v('v', 5),
    W('W', 5),
    w('w', 5),
    X('X', 5),
    x('x', 5),
    Y('Y', 5),
    y('y', 5),
    Z('Z', 5),
    z('z', 5),
    NUM_1('1', 5),
    NUM_2('2', 5),
    NUM_3('3', 5),
    NUM_4('4', 5),
    NUM_5('5', 5),
    NUM_6('6', 5),
    NUM_7('7', 5),
    NUM_8('8', 5),
    NUM_9('9', 5),
    NUM_0('0', 5),
    EXCLAMATION_POINT('!', 1),
    AT_SYMBOL('@', 6),
    NUM_SIGN('#', 5),
    DOLLAR_SIGN('$', 5),
    PERCENT('%', 5),
    UP_ARROW('^', 5),
    AMPERSAND('&', 5),
    ASTERISK('*', 5),
    LEFT_PARENTHESIS('(', 4),
    RIGHT_PERENTHESIS(')', 4),
    MINUS('-', 5),
    UNDERSCORE('_', 5),
    PLUS_SIGN('+', 5),
    EQUALS_SIGN('=', 5),
    LEFT_CURL_BRACE('{', 4),
    RIGHT_CURL_BRACE('}', 4),
    LEFT_BRACKET('[', 3),
    RIGHT_BRACKET(']', 3),
    COLON(':', 1),
    SEMI_COLON(';', 1),
    DOUBLE_QUOTE('"', 3),
    SINGLE_QUOTE('\'', 1),
    LEFT_ARROW('<', 4),
    RIGHT_ARROW('>', 4),
    QUESTION_MARK('?', 5),
    SLASH('/', 5),
    BACK_SLASH('\\', 5),
    LINE('|', 1),
    TILDE('~', 5),
    TICK('`', 2),
    PERIOD('.', 1),
    COMMA(',', 1),
    SPACE(' ', 3),
    DEFAULT('a', 4);

    val boldLength: Int
        get() {
            if (this == SPACE) return this.length
            return this.length + 1
        }

    companion object {
        fun getDefaultFontInfo(c: Char): DefaultFontEnum {
            for (dFI in entries) {
                if (dFI.character == c) return dFI
            }
            return DEFAULT
        }
    }
}
