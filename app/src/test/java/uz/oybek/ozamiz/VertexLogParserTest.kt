package uz.oybek.ozamiz

import org.junit.Test
import uz.oybek.ozamiz.util.VertexLogParser

class VertexLogParserTest {

    @Test
    fun parseLogs() {
        val inputFilePath = """C:\Users\samsu\Downloads\downloaded-logs-20260903-102956.json"""
        val outputDirectory = """C:\Users\samsu\OneDrive\Desktop\Ozamiz data"""
        VertexLogParser.processLogFile(inputFilePath, outputDirectory)
    }
}
