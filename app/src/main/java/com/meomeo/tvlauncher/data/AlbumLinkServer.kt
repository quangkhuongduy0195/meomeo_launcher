package com.meomeo.tvlauncher.data

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.SecureRandom
import kotlin.concurrent.thread

/**
 * A tiny web server that lets a phone on the same Wi-Fi type the album link, because typing a long
 * URL with a TV remote is miserable.
 *
 * The TV shows a QR code for [pageUrl]; the phone opens it, pastes the link into a form and
 * submits it. The server exists only while the "change album" dialog is open, and every URL
 * contains a random [token], so nobody who merely knows the TV's address can send it a link.
 *
 * Plain HTTP on the local network is intentional: there is no certificate a phone would trust for
 * a private address, and the form carries nothing secret.
 *
 * @param onLink called on a server thread with a validated https album link.
 * @param onPageOpened called when a phone loads the form, so the TV can show that it was reached.
 */
class AlbumLinkServer(
    private val context: Context,
    private val onLink: (String) -> Unit,
    private val onPageOpened: () -> Unit = {}
) {

    private val token: String = ByteArray(9).also { SecureRandom().nextBytes(it) }
        .joinToString("") { "%02x".format(it) }
    private var serverSocket: ServerSocket? = null

    /** Address the phone should open, or null when the TV has no network or the server could not start. */
    var pageUrl: String? = null
        private set

    /**
     * Other addresses the TV answers on (say it has both Ethernet and Wi-Fi). If the phone cannot
     * open [pageUrl], one of these is probably on the phone's network.
     */
    var alternateUrls: List<String> = emptyList()
        private set

    /** Starts listening. Returns false (and leaves [pageUrl] null) if there is no usable network. */
    fun start(): Boolean {
        val addresses = localAddresses()
        if (addresses.isEmpty()) return false
        val socket = openSocket() ?: return false
        serverSocket = socket
        val urls = addresses.map { "http://${it.hostAddress}:${socket.localPort}/$token" }
        pageUrl = urls.first()
        alternateUrls = urls.drop(1)
        thread(name = "AlbumLinkServer", isDaemon = true) {
            while (!socket.isClosed) {
                val client = try {
                    socket.accept()
                } catch (_: Exception) {
                    break
                }
                // One short-lived thread per request, so a stalled phone cannot block the others.
                thread(name = "AlbumLinkClient", isDaemon = true) { handle(client) }
            }
        }
        return true
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        pageUrl = null
        alternateUrls = emptyList()
    }

    /** A fixed port gives a short address that is easy to type if the QR code will not scan; any free port otherwise. */
    private fun openSocket(): ServerSocket? {
        for (port in intArrayOf(PREFERRED_PORT, 0)) {
            try {
                return ServerSocket(port)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot listen on port $port: ${e.message}")
            }
        }
        return null
    }

    private fun handle(client: Socket) {
        client.use { socket ->
            try {
                socket.soTimeout = 5_000
                val input = socket.getInputStream()
                val head = readHead(input) ?: return
                val lines = head.split("\r\n")
                val parts = lines.first().split(" ")
                if (parts.size < 2) return
                val method = parts[0]
                val path = parts[1].substringBefore('?')

                val contentLength = lines.firstNotNullOfOrNull {
                    if (it.startsWith("content-length:", ignoreCase = true)) it.substringAfter(':').trim().toIntOrNull() else null
                } ?: 0

                val out = socket.getOutputStream()
                when {
                    method == "GET" && path == "/$token" -> {
                        respond(out, 200, formPage(null))
                        onPageOpened()
                    }
                    method == "POST" && path == "/$token/set" -> {
                        if (contentLength > MAX_BODY) {
                            respond(out, 413, messagePage("Nội dung quá dài.", ok = false))
                            return
                        }
                        val body = String(readBody(input, contentLength), Charsets.UTF_8)
                        val raw = body.split('&')
                            .firstOrNull { it.startsWith("link=") }
                            ?.substringAfter('=')
                            ?.let { URLDecoder.decode(it, "UTF-8") }
                            .orEmpty()
                        val link = AlbumLinks.extract(raw)
                        if (link == null) {
                            respond(out, 400, formPage("Đây không phải link album Google Photos. Hãy dùng link dạng photos.app.goo.gl/..."))
                        } else {
                            onLink(link)
                            respond(out, 200, messagePage("Đã gửi tới TV. Hãy nhìn màn hình TV để xem tiến trình đồng bộ.", ok = true))
                        }
                    }
                    else -> respond(out, 404, messagePage("Không tìm thấy trang. Hãy quét lại mã QR trên TV.", ok = false))
                }
            } catch (e: Exception) {
                Log.d(TAG, "Request failed: ${e.message}")
            }
        }
    }

    /** Reads up to the blank line that ends the headers; null if the client sent too much or nothing. */
    private fun readHead(input: InputStream): String? {
        val buffer = ByteArrayOutputStream()
        var last4 = 0
        while (buffer.size() < MAX_HEAD) {
            val b = input.read()
            if (b < 0) return null
            buffer.write(b)
            last4 = (last4 shl 8) or b
            if (last4 == 0x0D0A0D0A) return buffer.toString(Charsets.ISO_8859_1.name()).trimEnd()
        }
        return null
    }

    private fun readBody(input: InputStream, length: Int): ByteArray {
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(body, read, length - read)
            if (n < 0) break
            read += n
        }
        return body.copyOf(read)
    }

    private fun respond(out: java.io.OutputStream, status: Int, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        val reason = when (status) {
            200 -> "OK"
            400 -> "Bad Request"
            404 -> "Not Found"
            else -> "Error"
        }
        out.write(
            ("HTTP/1.1 $status $reason\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Cache-Control: no-store\r\n" +
                "Connection: close\r\n\r\n").toByteArray(Charsets.ISO_8859_1)
        )
        out.write(bytes)
        out.flush()
    }

    private fun formPage(error: String?): String = page(
        """
        <h1>Đổi album Google Photos</h1>
        <p>Mở album trong Google Photos, chọn <b>Chia sẻ</b> rồi <b>Sao chép liên kết</b>, sau đó dán vào ô dưới đây.</p>
        ${if (error != null) "<p class=\"err\">$error</p>" else ""}
        <form method="post" action="/$token/set">
          <input name="link" type="text" inputmode="url" autocomplete="off" autocapitalize="off"
                 spellcheck="false" placeholder="https://photos.app.goo.gl/..." required autofocus>
          <button type="submit">Gửi tới TV</button>
        </form>
        """.trimIndent()
    )

    private fun messagePage(message: String, ok: Boolean): String = page(
        """
        <h1>${if (ok) "Đã gửi" else "Có lỗi"}</h1>
        <p class="${if (ok) "ok" else "err"}">$message</p>
        """.trimIndent()
    )

    private fun page(body: String): String = """
        <!doctype html><html lang="vi"><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>MeoMeo TV</title>
        <style>
          :root{color-scheme:light dark}
          body{font:16px/1.5 system-ui,sans-serif;margin:0;padding:24px;max-width:520px;margin:auto}
          h1{font-size:22px;margin:8px 0 12px}
          input{width:100%;box-sizing:border-box;padding:14px;font-size:16px;border-radius:12px;border:1px solid #8888;margin:12px 0}
          button{width:100%;padding:14px;font-size:17px;font-weight:600;border:0;border-radius:12px;background:#0a84ff;color:#fff}
          .err{color:#d70015}.ok{color:#1a8f3c}
        </style></head><body>$body</body></html>
    """.trimIndent()

    /**
     * The TV's private IPv4 addresses, best first. The address of the network the TV is actually
     * using comes first; that is the one a phone on the same Wi-Fi can reach. Interface names only
     * break ties, because TVs often have Ethernet, Wi-Fi and Wi-Fi Direct up at once.
     */
    private fun localAddresses(): List<Inet4Address> {
        val active = try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.getLinkProperties(cm.activeNetwork)?.linkAddresses
                ?.map { it.address }?.filterIsInstance<Inet4Address>()?.filter { it.isSiteLocalAddress }
                .orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        val byInterface = try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                // Wi-Fi Direct (p2p) groups use a private range too but no phone is on them.
                .filterNot { it.name.startsWith("p2p") }
                .flatMap { nic -> nic.inetAddresses.toList().filterIsInstance<Inet4Address>().filter { it.isSiteLocalAddress }.map { nic to it } }
                .sortedBy { (nic, _) -> if (nic.name.startsWith("wlan") || nic.name.startsWith("eth")) 0 else 1 }
                .map { it.second }
        } catch (_: Exception) {
            emptyList()
        }
        return (active + byInterface).distinctBy { it.hostAddress }
    }

    private companion object {
        const val TAG = "AlbumLinkServer"
        const val PREFERRED_PORT = 8765
        const val MAX_HEAD = 8 * 1024
        const val MAX_BODY = 4 * 1024
    }
}
