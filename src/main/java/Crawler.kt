import com.google.common.io.Files
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.jsoup.Jsoup
import java.io.File
import java.net.URL
import java.net.URLConnection

class Crawler {
    private val url = "https://ameblo.jp/yajima-maimi-official/entrylist.html"
    //获取所有的分页页面
    private fun getAllPages(): HashSet<String> {
        val pageSet = HashSet<String>()
        val document = Jsoup.connect(url).timeout(0).header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3298.3 Safari/537.36").get()
        val ul = document.select("ul[class='skin-paginationNums']").first()
        val hrefs = ul.select("li > a[href]")
        hrefs.mapTo(pageSet) { it.attr("abs:href") }
        return pageSet
    }
    //获取所有的文章url
    private fun getPageUrls(): HashSet<String> {
        val urlSet = HashSet<String>()
        for (allPage in getAllPages()) {
            val document = Jsoup.connect(allPage).timeout(0).header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3298.3 Safari/537.36").get()
            val titles = document.select("h2[data-uranus-component='entryItemTitle']")
            titles.mapTo(urlSet) { it.select("a[href]").first().attr("abs:href") }
        }
        return urlSet
    }
    //获取文章中所有的图片链接
    private fun getPicUrls(): HashSet<String> {
        val picUrlSets = HashSet<String>()
        for (pageUrl in getPageUrls()) {
            val httpClient: CloseableHttpClient = HttpClients.createDefault()
            val httpGet = HttpGet(pageUrl)
            val response: HttpResponse = httpClient.execute(httpGet)
            val result: String = EntityUtils.toString(response.entity, "UTF-8")

            val document = Jsoup.parse(result)
            val div = document.select("div[class='skin-entryBody']").first()
            val imgs = div.select("a > img[src]")
            if (imgs != null && imgs.size > 0) {
//                println(imgs.attr("src"))
                imgs.mapTo(picUrlSets) { it.attr("src") }
            }
        }
        return picUrlSets
    }
    //根据图片链接下载图片
    fun downLoadPic() {
        for (picUrl in getPicUrls()) {
            val filename = picUrl.substring(picUrl.lastIndexOf("/")+1, picUrl.lastIndexOf("?"))
            val url:URL = URL(picUrl)
            val con: URLConnection = url.openConnection()
            val inputStream = con.getInputStream()
            val file = File(filename)
            Files.write(inputStream.readBytes(), file)
            println("pic:$filename is downloaded !")
        }

    }
}