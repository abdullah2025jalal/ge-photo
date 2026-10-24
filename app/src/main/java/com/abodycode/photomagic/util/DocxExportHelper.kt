package com.abodycode.photomagic.util

import android.content.Context
import android.graphics.Bitmap
import com.abodycode.photomagic.model.A4GridPlan
import com.abodycode.photomagic.model.PrintPhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxExportHelper {

    /**
     * Generates a Microsoft Word .docx file formatted to an A4 page,
     * containing multi-photo grid with front and back document faces
     * placed "In Front of Text" (<wp:anchor behindDoc="0">)
     * with EMU coordinate conversions, 1 mm spacing, 0.6 cm margins, and zero dashed borders.
     */
    suspend fun generateMultiPhotoDocx(
        context: Context,
        plan: A4GridPlan,
        fileName: String = "PhotoMagic_A4_Multi.docx"
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(outputDir, fileName)

        // Map every unique face (item_id + front/back) to a relationship ID and its PNG bytes
        val faceKeys = mutableListOf<Pair<String, Bitmap>>()
        val faceToRelId = mutableMapOf<String, String>()

        for (placed in plan.placedPhotos) {
            val item = placed.photoItem
            val isBack = placed.isBackSide
            val faceKey = "${item.id}_${if (isBack) "back" else "front"}"
            var bitmap = if (isBack) item.backBitmap else item.sourceBitmap

            if (bitmap == null && isBack) {
                // Generate a clean placeholder bitmap for the back face of document
                val phW = placed.widthPx(300).coerceAtLeast(300)
                val phH = placed.heightPx(300).coerceAtLeast(200)
                val phBmp = Bitmap.createBitmap(phW, phH, Bitmap.Config.ARGB_8888)
                val c = android.graphics.Canvas(phBmp)
                c.drawColor(android.graphics.Color.parseColor("#F8FAFC"))
                val borderP = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#CBD5E1")
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 4f
                }
                c.drawRect(0f, 0f, phW.toFloat(), phH.toFloat(), borderP)
                val textP = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#64748B")
                    textSize = 28f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                c.drawText("الوجه الخلفي للمستند", phW / 2f, phH / 2f + 10f, textP)
                bitmap = phBmp
            }

            if (bitmap != null && !faceToRelId.containsKey(faceKey)) {
                val relId = "rId${faceKeys.size + 1}"
                faceToRelId[faceKey] = relId
                faceKeys.add(Pair(faceKey, bitmap))
            }
        }

        val itemToImageBytes = mutableMapOf<String, ByteArray>()
        faceKeys.forEachIndexed { index, pair ->
            val faceKey = pair.first
            val originalBmp = pair.second
            val item = plan.placedPhotos.first { "${it.photoItem.id}_${if (it.isBackSide) "back" else "front"}" == faceKey }.photoItem
            val cropped = BitmapProcessor.cropToAspectRatio(originalBmp, item.aspectRatio)
            val stream = ByteArrayOutputStream()
            cropped.compress(Bitmap.CompressFormat.PNG, 100, stream)
            itemToImageBytes[faceKey] = stream.toByteArray()
        }

        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // 1. [Content_Types].xml
                writeZipEntry(zos, "[Content_Types].xml", buildContentTypesXml())

                // 2. _rels/.rels
                writeZipEntry(zos, "_rels/.rels", buildGlobalRelsXml())

                // 3. word/_rels/document.xml.rels
                writeZipEntry(zos, "word/_rels/document.xml.rels", buildDocumentRelsXml(faceKeys, faceToRelId))

                // 4. word/media/image_{index}.png for each unique photo face
                faceKeys.forEachIndexed { index, pair ->
                    val imageBytes = itemToImageBytes[pair.first] ?: ByteArray(0)
                    val entry = ZipEntry("word/media/image_${index + 1}.png")
                    zos.putNextEntry(entry)
                    zos.write(imageBytes)
                    zos.closeEntry()
                }

                // 5. word/document.xml
                writeZipEntry(zos, "word/document.xml", buildDocumentXml(plan, faceToRelId))
            }
        }

        outputFile
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        zos.write(content.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
    }

    private fun buildContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="png" ContentType="image/png"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".trimIndent()

    private fun buildGlobalRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()

    private fun buildDocumentRelsXml(
        faceKeys: List<Pair<String, Bitmap>>,
        faceToRelId: Map<String, String>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
""")
        faceKeys.forEachIndexed { index, pair ->
            val relId = faceToRelId[pair.first] ?: "rId${index + 1}"
            val mediaPath = "media/image_${index + 1}.png"
            sb.append("""  <Relationship Id="$relId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="$mediaPath"/>
""")
        }
        sb.append("</Relationships>")
        return sb.toString()
    }

    private fun buildDocumentXml(
        plan: A4GridPlan,
        faceToRelId: Map<String, String>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
            xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
  <w:body>
""")

        // Group photos by page index
        val pages = plan.placedPhotos.groupBy { it.pageIndex }.toSortedMap()

        pages.forEach { (pageIdx, photosOnPage) ->
            if (pageIdx > 0) {
                // Page break before next page
                sb.append("""
    <w:p>
      <w:r>
        <w:br w:type="page"/>
      </w:r>
    </w:p>
""")
            }

            sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/>
      </w:pPr>
""")

            for (placed in photosOnPage) {
                val docPrId = placed.instanceIndex + 1
                val faceKey = "${placed.photoItem.id}_${if (placed.isBackSide) "back" else "front"}"
                val relId = faceToRelId[faceKey] ?: "rId1"

                sb.append("""
      <w:r>
        <w:drawing>
          <wp:anchor distT="0" distB="0" distL="0" distR="0" simplePos="0" relativeHeight="251658240" behindDoc="0" locked="0" layoutInCell="1" allowOverlap="1">
            <wp:simplePos x="0" y="0"/>
            <wp:positionH relativeFrom="page">
              <wp:posOffset>${placed.xEmu}</wp:posOffset>
            </wp:positionH>
            <wp:positionV relativeFrom="page">
              <wp:posOffset>${placed.yEmu}</wp:posOffset>
            </wp:positionV>
            <wp:extent cx="${placed.widthEmu}" cy="${placed.heightEmu}"/>
            <wp:effectExtent l="0" t="0" r="0" b="0"/>
            <wp:wrapNone/>
            <wp:docPr id="$docPrId" name="Photo_$docPrId"/>
            <wp:cNvGraphicFramePr>
              <a:graphicFrameLocks noChangeAspect="1"/>
            </wp:cNvGraphicFramePr>
            <a:graphic>
              <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                <pic:pic>
                  <pic:nvPicPr>
                    <pic:cNvPr id="$docPrId" name="Photo_$docPrId"/>
                    <pic:cNvPicPr>
                      <a:picLocks noChangeAspect="1"/>
                    </pic:cNvPicPr>
                  </pic:nvPicPr>
                  <pic:blipFill>
                    <a:blip r:embed="$relId"/>
                    <a:stretch>
                      <a:fillRect/>
                    </a:stretch>
                  </pic:blipFill>
                  <pic:spPr>
                    <a:xfrm>
                      <a:off x="0" y="0"/>
                      <a:ext cx="${placed.widthEmu}" cy="${placed.heightEmu}"/>
                    </a:xfrm>
                    <a:prstGeom prst="rect">
                      <a:avLst/>
                    </a:prstGeom>
                    <a:ln>
                      <a:noFill/>
                    </a:ln>
                  </pic:spPr>
                </pic:pic>
              </a:graphicData>
            </a:graphic>
          </wp:anchor>
        </w:drawing>
      </w:r>
""")
            }

            sb.append("""
    </w:p>
""")
        }

        // Section properties: A4 page size (11906 x 16838 dxa), margin 0.6 cm = 340 dxa
        sb.append("""
    <w:sectPr>
      <w:pgSz w:w="11906" w:h="16838" w:orient="portrait"/>
      <w:pgMar w:top="340" w:right="340" w:bottom="340" w:left="340" w:header="0" w:footer="0" w:gutter="0"/>
    </w:sectPr>
  </w:body>
</w:document>
""")
        return sb.toString()
    }
}
