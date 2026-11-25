package tw.repairtrack.app

import java.io.*
import java.util.zip.*
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

object Excel {
 private fun xml(b:ByteArray) = DocumentBuilderFactory.newInstance().apply { isNamespaceAware=false; setFeature("http://apache.org/xml/features/disallow-doctype-decl",true) }.newDocumentBuilder().parse(b.inputStream())
 private fun Element.tags(n:String)=getElementsByTagName(n).let { l -> (0 until l.length).map { l.item(it) as Element } }
 fun read(input:InputStream):List<Map<String,String>> {
  val files=mutableMapOf<String,ByteArray>(); var total=0L
  ZipInputStream(input).use { z -> while(true){ val e=z.nextEntry?:break; val out=ByteArrayOutputStream(); val buf=ByteArray(8192); while(true){val n=z.read(buf);if(n<0)break;total+=n;require(total<100_000_000){"Excel 解壓大小超限"};out.write(buf,0,n)};files[e.name]=out.toByteArray() } }
  fun doc(p:String)=xml(requireNotNull(files[p]){"缺少 $p"}).documentElement
  val shared=files["xl/sharedStrings.xml"]?.let { xml(it).documentElement.tags("si").map { e->e.tags("t").joinToString(""){it.textContent} } }?:emptyList()
  val rel=doc("xl/_rels/workbook.xml.rels").tags("Relationship").associate { it.getAttribute("Id") to it.getAttribute("Target") }
  fun rows(name:String):List<Map<String,String>> { val sheet=doc("xl/workbook.xml").tags("sheet").first { it.getAttribute("name").trim()==name }; val target=rel.getValue(sheet.getAttribute("r:id")); val path=if(target.startsWith("/"))target.drop(1) else "xl/$target"; return doc(path).tags("row").map { row -> row.tags("c").associate { c -> val v=c.tags("v").firstOrNull()?.textContent.orEmpty(); c.getAttribute("r").takeWhile{it.isLetter()} to when(c.getAttribute("t")){"s"->shared.getOrElse(v.toIntOrNull()?:-1){""};"inlineStr"->c.tags("t").joinToString(""){it.textContent};"e"->"";else->v} } } }
  val params=rows("參數").filter{it["A"]?.toDoubleOrNull()!=null}.associateBy{it["A"]!!.removeSuffix(".0")}; val seen=mutableMapOf<String,Int>()
  return rows("售服缺失總表(住戶端)").filter{!it["C"].isNullOrBlank()&&!it["E"].isNullOrBlank()&&it["A"]?.toDoubleOrNull()!=null}.map { r ->
   fun v(c:String)=r[c].orEmpty().trim()
   val p=params[v("G").removeSuffix(".0")]; val key=listOf(v("A"),v("C"),v("D"),v("E")).joinToString("|"); val seq=(seen[key]?:0)+1;seen[key]=seq
   val id=MessageDigest.getInstance("SHA-256").digest("$key|$seq".toByteArray()).joinToString(""){"%02x".format(it)}
   mapOf("id" to id,"ticket" to v("A"),"date" to v("B"),"unit" to v("C"),"floor" to v("D"),"description" to v("E"),"reporter" to v("F"),"parameter" to v("G"),"vendor" to (v("H").ifBlank{p?.get("B").orEmpty()}).ifBlank{"未分類"},"trade" to p?.get("C").orEmpty().ifBlank{"未分類"},"handover" to v("I"),"status" to if(v("K").toDoubleOrNull()==1.0)"已銷單" else "未銷單","completed" to v("L"),"contact" to v("M"),"access" to v("N"))
  }
 }
 private fun esc(s:String)=s.filter{it=='\n'||it=='\r'||it=='\t'||it>=' '}.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
 fun write(output:OutputStream,sheets:Map<String,List<List<String>>>) { ZipOutputStream(output).use { z -> fun put(n:String,s:String){z.putNextEntry(ZipEntry(n));z.write(s.toByteArray());z.closeEntry()}; val ns="http://schemas.openxmlformats.org/spreadsheetml/2006/main";val rel="http://schemas.openxmlformats.org/officeDocument/2006/relationships"; val pkg="http://schemas.openxmlformats.org/package/2006/relationships"
 put("[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"+sheets.keys.indices.joinToString(""){"<Override PartName=\"/xl/worksheets/sheet${it+1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"}+"</Types>")
 put("_rels/.rels","<Relationships xmlns=\"$pkg\"><Relationship Id=\"rId1\" Type=\"$rel/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
 put("xl/workbook.xml","<workbook xmlns=\"$ns\" xmlns:r=\"$rel\"><sheets>"+sheets.keys.mapIndexed{i,n->"<sheet name=\"${esc(n)}\" sheetId=\"${i+1}\" r:id=\"rId${i+1}\"/>"}.joinToString("")+"</sheets></workbook>")
 put("xl/_rels/workbook.xml.rels","<Relationships xmlns=\"$pkg\">"+sheets.keys.indices.joinToString(""){"<Relationship Id=\"rId${it+1}\" Type=\"$rel/worksheet\" Target=\"worksheets/sheet${it+1}.xml\"/>"}+"</Relationships>")
 sheets.values.forEachIndexed { i,rows -> put("xl/worksheets/sheet${i+1}.xml","<worksheet xmlns=\"$ns\"><sheetData>"+rows.mapIndexed{j,row->"<row r=\"${j+1}\">"+row.mapIndexed{k,v->"<c r=\"${('A'.code+k).toChar()}${j+1}\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v)}</t></is></c>"}.joinToString("")+"</row>"}.joinToString("")+"</sheetData></worksheet>") }
 } }
}
