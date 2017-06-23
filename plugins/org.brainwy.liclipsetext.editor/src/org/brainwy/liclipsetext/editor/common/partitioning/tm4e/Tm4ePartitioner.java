package org.brainwy.liclipsetext.editor.common.partitioning.tm4e;

import java.util.HashMap;
import java.util.Map;

import org.brainwy.liclipsetext.editor.partitioning.DocumentTimeStampChangedException;
import org.brainwy.liclipsetext.editor.partitioning.ScannerRange;
import org.brainwy.liclipsetext.shared_core.log.Log;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.grammar.ITokenizeLineResult;
import org.eclipse.tm4e.core.grammar.StackElement;

public class Tm4ePartitioner implements IDocumentPartitioner {

    private static final String TM4E_LICLIPSE_PARTITIONING = "TM4E_LICLIPSE_PARTITIONING";
    private static final Object addPartitionerLock = new Object();

    public static synchronized Tm4ePartitioner getTm4eDocumentPartitioner(IDocument doc) {
        IDocumentExtension3 docExt3 = (IDocumentExtension3) doc;
        Tm4ePartitioner documentPartitioner = (Tm4ePartitioner) docExt3
                .getDocumentPartitioner(TM4E_LICLIPSE_PARTITIONING);
        if (documentPartitioner == null) {
            synchronized (addPartitionerLock) {
                documentPartitioner = (Tm4ePartitioner) docExt3.getDocumentPartitioner(TM4E_LICLIPSE_PARTITIONING);
                if (documentPartitioner == null) {
                    documentPartitioner = new Tm4ePartitioner();
                    try {
                        documentPartitioner.connect(doc);
                    } catch (Exception e) {
                        Log.log("Error connecting partitioner", e);
                    }
                    docExt3.setDocumentPartitioner(TM4E_LICLIPSE_PARTITIONING, documentPartitioner);
                }
                return documentPartitioner;
            }
        } else {
            return documentPartitioner;
        }
    }

    private IDocument fDocument;

    @Override
    public void connect(IDocument document) {
        this.fDocument = document;
    }

    @Override
    public void disconnect() {
        this.fDocument = null;
    }

    @Override
    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    @Override
    public boolean documentChanged(DocumentEvent event) {
        int replacedTextLen = event.getLength();
        int offset = event.getOffset();
        String text = event.getText();
        if (replacedTextLen > 0) {
            // ... Finish to invalidate caches.
        }
        return false;
    }

    @Override
    public String[] getLegalContentTypes() {
        return null;
    }

    @Override
    public String getContentType(int offset) {
        return null;
    }

    @Override
    public ITypedRegion[] computePartitioning(int offset, int length) {
        return null;
    }

    @Override
    public ITypedRegion getPartition(int offset) {
        return null;
    }

    private static class Tm4eLineInfo {

    }

    private static class Tm4eDocCache {
        //        private LRUCacheWithSoftPrunedValues<Tm4eCacheKey, Tm4eLineInfo> prevStateAndLineContentsToInfo = new LRUCacheWithSoftPrunedValues<>(
        //                10000);
        //
        public synchronized void updateFrom(IGrammar grammar, ScannerRange scannerRange) {
            //            Tm4eScannerCache tm4eCache = (Tm4eScannerCache) scannerRange.tm4eCache;
            //            if (tm4eCache != null) {
            //                Set<Entry<Tm4eCacheKey, Tm4eLineInfo>> entrySet = tm4eCache.prevStateAndLineContentsToInfo.entrySet();
            //                for (Entry<Tm4eCacheKey, Tm4eLineInfo> entry : entrySet) {
            //                    prevStateAndLineContentsToInfo.put(entry.getKey(), entry.getValue());
            //                }
            //            }
        }
    }

    private Tm4eDocCache docCache = new Tm4eDocCache();

    private static class Tm4eCacheKey {

    }

    /**
     * A cache that lives inside the scanner (should always go forward and when finished scanning it updates the Tm4eDocCache).
     */
    private static class Tm4eScannerCache {
        public StackElement prevState;
        private Map<Tm4eCacheKey, Tm4eLineInfo> prevStateAndLineContentsToInfo = new HashMap<>();
        private StackElement[] lines;
        private int startLine;
        private int endLine;
    }

    public ITokenizeLineResult tokenizeLine(int lineFromOffset, String lineContents, IGrammar grammar,
            ScannerRange scannerRange) throws DocumentTimeStampChangedException {
        StackElement prevState = null;
        Tm4eScannerCache tm4eCache = (Tm4eScannerCache) scannerRange.tm4eCache;
        if (tm4eCache == null) {
            tm4eCache = (Tm4eScannerCache) (scannerRange.tm4eCache = new Tm4eScannerCache());

            try {
                int startLine = scannerRange.getLineFromOffset(scannerRange.getRangeStartOffset());
                int endLine = scannerRange.getLineFromOffset(scannerRange.getRangeEndOffset());
                if (endLine < startLine) {
                    endLine = startLine;
                }
                int nLines = endLine - startLine;
                tm4eCache.lines = new StackElement[nLines];
                tm4eCache.startLine = startLine;
                tm4eCache.endLine = endLine;
            } catch (BadLocationException e) {
                scannerRange.checkDocumentTimeStampChanged();
                Log.log(e);
            }
            prevState = null;
        } else {
            prevState = tm4eCache.prevState;
        }
        ITokenizeLineResult ret = grammar.tokenizeLine(lineContents, prevState);
        tm4eCache.prevState = ret.getRuleStack();
        return ret;
    }

    public void finishTm4ePartition(IGrammar grammar, ScannerRange scannerRange) {
        // At this point we have to persist the parsing info to the document so that we can restart the
        // partitioning later on.
        docCache.updateFrom(grammar, scannerRange);
    }

}