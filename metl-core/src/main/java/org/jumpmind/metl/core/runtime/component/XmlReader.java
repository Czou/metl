package org.jumpmind.metl.core.runtime.component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class XmlReader extends AbstractComponentRuntime {

	public static final String TYPE = "XmlProcessor";

	public final static String SETTING_GET_FILE_FROM_MESSAGE = "get.file.name.from.message";

	public final static String SETTING_RELATIVE_PATH = "relative.path";

	public final static String SETTING_READ_TAG = "read.tag";

	public final static String SETTING_READ_TAGS_PER_MESSAGE = "read.tags.per.message";

	boolean getFileNameFromMessage = false;

	String relativePathAndFile;

	String readTag;

	int readTagsPerMessage = 1;

	@Override
	protected void start() {
		Component component = getComponent();
		getFileNameFromMessage = component.getBoolean(SETTING_GET_FILE_FROM_MESSAGE, getFileNameFromMessage);
		relativePathAndFile = component.get(SETTING_RELATIVE_PATH, relativePathAndFile);
		readTagsPerMessage = component.getInt(SETTING_READ_TAGS_PER_MESSAGE, readTagsPerMessage);
		readTag = component.get(SETTING_READ_TAG, readTag);
	}

	@Override
	public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
		List<String> files = getFilesToRead(inputMessage);
		try {
			processFiles(files, callback, unitOfWorkBoundaryReached);
		} catch (Exception e) {
			throw new IoException(e);
		}

	}

	List<String> getFilesToRead(Message inputMessage) {
		ArrayList<String> files = new ArrayList<String>();
		if (getFileNameFromMessage) {
			List<String> fullyQualifiedFiles = inputMessage.getPayload();
			files.addAll(fullyQualifiedFiles);
		} else {
			files.add(relativePathAndFile);
		}
		return files;
	}

	void processFiles(List<String> files, ISendMessageCallback callback, boolean unitOfWorkLastMessage)
			throws XmlPullParserException, IOException {
		XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
		ArrayList<String> outboundPayload = new ArrayList<String>();

		IResourceRuntime resourceRuntime = getResourceRuntime();
        String path = resourceRuntime.getResourceRuntimeSettings().get(LocalFile.LOCALFILE_PATH);
		
		for (String file : files) {
			File xmlFile = null;
			if (!getFileNameFromMessage) {
				xmlFile = new File(path, file);
			}
			else {
				xmlFile = new File(file);
			}
			parser.setInput(new FileReader(xmlFile));
			LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file));
			lineNumberReader.setLineNumber(1);
			int startCol = 0;
			int startLine = 0;
			int prevEndLine = 0;
			int prevEndCol = 0;
			int eventType = parser.getEventType();
			String line = null;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					if (readTag == null) {
						readTag = parser.getName();
						info("Read tag was not set, defaulting to root tag: " + readTag);
					}
					if (parser.getName().equals(readTag)) {
						startCol = prevEndCol;
						startLine = prevEndLine;
					}
					break;
				case XmlPullParser.END_TAG:
					prevEndCol = parser.getColumnNumber();
					prevEndLine = parser.getLineNumber();
					if (parser.getName().equals(readTag)) {
						StringBuilder xml = new StringBuilder();
						forward(startLine - lineNumberReader.getLineNumber(), lineNumberReader);
						int linesToRead = parser.getLineNumber() - lineNumberReader.getLineNumber();
						if (startLine >= lineNumberReader.getLineNumber()) {
							line = lineNumberReader.readLine();
						} else {
							linesToRead++;
						}
						while (linesToRead >= 0 && line != null) {
							if (startCol > 0) {
								if (line.length() > startCol) {
									xml.append(line.substring(startCol)).append("\n");
								}
								startCol = 0;
							} else if (linesToRead == 0) {
								if (line.length() > parser.getColumnNumber()) {
									xml.append(line.substring(0, parser.getColumnNumber()));
								} else {
									xml.append(line).append("\n");
								}
							} else {
								xml.append(line).append("\n");
							}

							linesToRead--;
							if (linesToRead >= 0) {
								line = lineNumberReader.readLine();
							}
						}
						getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
						outboundPayload.add(xml.toString());
						if (outboundPayload.size() == readTagsPerMessage) {
							callback.sendMessage(outboundPayload, false);
							outboundPayload = new ArrayList<String>();
						}
					}
					break;
				}
				eventType = parser.next();
			}

			IOUtils.closeQuietly(lineNumberReader);
		}
		callback.sendMessage(outboundPayload, unitOfWorkLastMessage);
	}

	protected static void forward(int lines, LineNumberReader lineNumberReader) throws IOException {
		while (lines > 0 && (lineNumberReader.readLine()) != null) {
			lines--;
		}
	}

	@Override
	public boolean supportsStartupMessages() {
		return false;
	}

}