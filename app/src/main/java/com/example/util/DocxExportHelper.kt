package com.example.util

import android.content.Context
import android.graphics.Bitmap
import com.example.model.A4GridPlan
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
     * containing the grid of photos positioned "In Front of Text" (<wp:anchor behindDoc="0">)
     * with EMU coordinate conversions and 3 mm cut spacing.
     */
    suspend fun generateDocx(
        context: Context,
        photoBitmap: Bitmap,
        plan: A4GridPlan,
        fileName: String = "ID_Photos_A4_Grid.docx"
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(outputDir, fileName)

        // Compress photo to PNG bytes
        val imageBytesStream = ByteArrayOutputStream()
        photoBitmap.compress(Bitmap.CompressFormat.PNG, 100, imageBytesStream)
        val imageBytes = imageBytesStream.toByteArray()

        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // 1. [Content_Types].xml
                writeZipEntry(zos, "[Content_Types].xml", buildContentTypesXml())

                // 2. _rels/.rels
                writeZipEntry(zos, "_rels/.rels", buildGlobalRelsXml())

                // 3. word/_rels/document.xml.rels
                writeZipEntry(zos, "word/_rels/document.xml.rels", buildDocumentRelsXml())

                // 4. word/media/image1.png
                val imageEntry = ZipEntry("word/media/image1.png")
                zos.putNextEntry(imageEntry)
                zos.write(imageBytes)
                zos.closeEntry()

                // 5. word/document.xml
                writeZipEntry(zos, "word/document.xml", buildDocumentXml(plan))
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

    private fun buildDocumentRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image1.png"/>
</Relationships>""".trimIndent()

    private fun buildDocumentXml(plan: A4GridPlan): String {
        // A4 page width: 21 cm = 11906 dxa, height: 29.7 cm = 16838 dxa (20ths of a pt)
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
            xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
  <w:body>
""")

        // Place each photo as a floating drawing "In Front of Text"
        for (cell in plan.cells) {
            val docPrId = cell.index + 1
            sb.append("""
    <w:p>
      <w:pPr>
        <w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/>
      </w:pPr>
      <w:r>
        <w:drawing>
          <wp:anchor distT="0" distB="0" distL="0" distR="0" simplePos="0" relativeHeight="251658240" behindDoc="0" locked="0" layoutInCell="1" allowOverlap="1">
            <wp:simplePos x="0" y="0"/>
            <wp:positionH relativeFrom="page">
              <wp:posOffset>${cell.xEmu}</wp:posOffset>
            </wp:positionH>
            <wp:positionV relativeFrom="page">
              <wp:posOffset>${cell.yEmu}</wp:posOffset>
            </wp:positionV>
            <wp:extent cx="${cell.widthEmu}" cy="${cell.heightEmu}"/>
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
                    <a:blip r:embed="rId1"/>
                    <a:stretch>
                      <a:fillRect/>
                    </a:stretch>
                  </pic:blipFill>
                  <pic:spPr>
                    <a:xfrm>
                      <a:off x="0" y="0"/>
                      <a:ext cx="${cell.widthEmu}" cy="${cell.heightEmu}"/>
                    </a:xfrm>
                    <a:prstGeom prst="rect">
                      <a:avLst/>
                    </a:prstGeom>
                    <a:ln w="9525">
                      <a:solidFill>
                        <a:srgbClr val="D0D5DD"/>
                      </a:solidFill>
                      <a:prstDash val="dash"/>
                    </a:ln>
                  </pic:spPr>
                </pic:pic>
              </a:graphicData>
            </a:graphic>
          </wp:anchor>
        </w:drawing>
      </w:r>
    </w:p>
""")
        }

        // Section properties with A4 page size
        sb.append("""
    <w:sectPr>
      <w:pgSz w:w="11906" w:h="16838" w:orient="portrait"/>
      <w:pgMar w:top="567" w:right="567" w:bottom="567" w:left="567" w:header="0" w:footer="0" w:gutter="0"/>
    </w:sectPr>
  </w:body>
</w:document>
""")
        return sb.toString()
    }
}
