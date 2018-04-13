package net.xdevelop.go.train;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.datavec.api.conf.Configuration;
import org.datavec.api.records.Record;
import org.datavec.api.records.metadata.RecordMetaData;
import org.datavec.api.records.metadata.RecordMetaDataLine;
import org.datavec.api.records.reader.BaseRecordReader;
import org.datavec.api.records.reader.impl.LineRecordReader;
import org.datavec.api.split.InputSplit;
import org.datavec.api.split.InputStreamInputSplit;
import org.datavec.api.split.StringSplit;
import org.datavec.api.writable.FloatWritable;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 10 features reader
 *
 */
public class FeatureRecordReader extends BaseRecordReader {
	private static final long serialVersionUID = 6485321472559506918L;
	public final static String LABEL_DELIMITER = ",";
	private static Logger log = LoggerFactory.getLogger(FeatureRecordReader.class);
	private static Random RANDOM = new Random();
	
    private Iterator<List<Writable>> iter;
    private Iterator<String> lineIter;
    protected URI[] locations;
    protected int splitIndex = 0;
    protected int lineIndex = 0; //Line index within the current split
    protected Configuration conf;
    protected InputSplit inputSplit;
    private int channels = 10;

    public FeatureRecordReader() {
    	super();
    }
    
    @Override
    public void initialize(InputSplit split) throws IOException, InterruptedException {
        this.inputSplit = split;
        this.iter = getIterator(0);
    }

    @Override
    public void initialize(Configuration conf, InputSplit split) throws IOException, InterruptedException {
        initialize(split);
    }
    
    public void initialize(InputSplit split, int channels) throws IOException, InterruptedException {
    	initialize(split);
    	this.channels = channels;
    }

    private int count = 0;
    @Override
    public List<Writable> next() {
        List<Writable> ret = new ArrayList<>();
        if (count % 1000 == 0) {
        	log.info((new Date()) + " Data " + count);
        }
        count++;
        if (iter.hasNext()) {
            ret = iter.next();
            invokeListeners(ret);
            lineIndex++;
            return ret;
        } else {
            if (!(inputSplit instanceof StringSplit) && splitIndex < locations.length - 1) {
                splitIndex++;
                lineIndex = 0;
                try {
                    close();
                    iter = lineIterator(new InputStreamReader(locations[splitIndex].toURL().openStream()));
                    onLocationOpen(locations[splitIndex]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                lineIndex = 0; //New split opened -> reset line index

                if (iter.hasNext()) {
                    ret = iter.next();
                    invokeListeners(ret);
                    lineIndex++;
                    return ret;
                }
            }

            throw new NoSuchElementException("No more elements found!");
        }
    }
    
    private Iterator<List<Writable>> lineIterator(InputStreamReader reader) {
    	lineIter = IOUtils.lineIterator(reader);
    	
    	return new Iterator<List<Writable>>() {
			@Override
			public boolean hasNext() {
				return lineIter.hasNext();
			}

			@Override
			public List<Writable> next() {
				return parseLine(lineIter.next());
			}
    	};
    }
    
    protected List<Writable> parseLine(String line) {
        String[] split = line.split(LABEL_DELIMITER, -1);
    	float clsLabel = Float.parseFloat(split[0]);
    	float regLabel = Float.parseFloat(split[1]);
    	
        List<Writable> ret = new ArrayList<>();
        char[] chs = split[2].toCharArray();
        int rotation = RANDOM.nextInt(8);
        chs = transfer(chs, rotation);

        int v = 0;
    	for (int i = 0; i < chs.length; i++) {
    		v = Integer.parseInt(Character.toString(chs[i]));
    		if (i < 8 * 361 || i >= 9 * 361) {
    			ret.add(new FloatWritable(v));
    		}
    		else {
	    		if (v > 1) {
	    			ret.add(new FloatWritable((float)(Math.exp(-(v-1) * 0.1))));
	    		}
	    		else {
	    			ret.add(new FloatWritable(v));
	    		}
    		}
    	}
        ret.add(new IntWritable(rotateIndex((int) clsLabel, rotation))); // class label
        ret.add(new FloatWritable(regLabel)); // value regression label
        
        return ret;
    }
    
    private char[] transfer(char[] chs, int rotationMode) {
    	int index = 0;
    	int len = chs.length;
    	char[] newChs = new char[len];
    	for (int n = 0; n < channels; n++) {
	    	for (int i = 0; i < 361; i++) {
	    		index = rotateIndex(i, rotationMode);
	    		newChs[n * 361 + index] = chs[n * 361 + i];
	    	}
    	}
    	return newChs;
    }
    
    private int rotateIndex(int originIndex, int rotationMode) {
    	int index = 0;
		int y = originIndex / 19;
		int x = originIndex - y * 19;

		switch (rotationMode) {
		case 0:
			index = originIndex;
			break;
		case 1:
			index = y * 19 + 19 - x - 1;
			break;
		case 2:
			index = (19 - y - 1) * 19 + x;
			break;
		case 3:
			index = x * 19 + y;
			break;
		case 4:
			index = x * 19 + 19 - y - 1;
			break;
		case 5:
			index = (19 - x - 1) * 19 + y;
			break;
		case 6:
			index = (19 - y - 1) * 19 + 19 - x - 1;
			break;
		case 7:
			index = (19 - x - 1) * 19 + 19 - y - 1;
			break;
		default:
			index = originIndex;
		}
		
		return index;
    }

    @Override
    public boolean hasNext() {
        if (iter != null && iter.hasNext()) {
            return true;
        } else {
            if (locations != null && !(inputSplit instanceof StringSplit) && splitIndex < locations.length - 1) {
                splitIndex++;
                lineIndex = 0; //New split -> reset line count
                try {
                    close();
                    iter = lineIterator(new InputStreamReader(locations[splitIndex].toURL().openStream()));
                    onLocationOpen(locations[splitIndex]);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return iter.hasNext();
            }

            return false;
        }
    }

    protected void onLocationOpen(URI location) {
    	log.info("loaded " + location.toString());
    }

    @Override
    public void close() throws IOException {
    	if (lineIter != null) {
            if (lineIter instanceof LineIterator) {
                LineIterator iter2 = (LineIterator) lineIter;
                iter2.close();
            }
        }
    }

    @Override
    public void setConf(Configuration conf) {
        this.conf = conf;
    }

    @Override
    public Configuration getConf() {
        return conf;
    }

    @Override
    public List<String> getLabels() {
        return null;
    }

    @Override
    public void reset() {
        if (inputSplit == null)
            throw new UnsupportedOperationException("Cannot reset without first initializing");
        try {
            initialize(inputSplit);
            splitIndex = 0;
        } catch (Exception e) {
            throw new RuntimeException("Error during LineRecordReader reset", e);
        }
        lineIndex = 0;
    }

    @Override
    public List<Writable> record(URI uri, DataInputStream dataInputStream) throws IOException {
        invokeListeners(uri);
        BufferedReader br = new BufferedReader(new InputStreamReader(dataInputStream));
        List<Writable> line = parseLine(br.readLine());
        return line;
    }

    protected Iterator<List<Writable>> getIterator(int location) {
    	Iterator<List<Writable>> iterator = null;
    	
    	if (inputSplit instanceof InputStreamInputSplit) {
            InputStream is = ((InputStreamInputSplit) inputSplit).getIs();
            if (is != null) {
                iterator = lineIterator(new InputStreamReader(is));
            }
        } else {
	        this.locations = inputSplit.locations();
	        if (locations != null && locations.length > 0) {
	            InputStream inputStream;
	            try {
	                inputStream = locations[location].toURL().openStream();
	                onLocationOpen(locations[location]);
	            } catch (IOException e) {
	                throw new RuntimeException(e);
	            }
	            iterator = lineIterator(new InputStreamReader(inputStream));
	        }
        }
        if (iterator == null)
            throw new UnsupportedOperationException("Unknown input split: " + inputSplit);
        return iterator;
    }

    protected void closeIfRequired(Iterator<String> iterator) {
        if (iterator instanceof LineIterator) {
            LineIterator iter = (LineIterator) iterator;
            iter.close();
        }
    }

    @Override
    public Record nextRecord() {
        List<Writable> next = next();
        URI uri = (locations == null || locations.length < 1 ? null : locations[splitIndex]);
        RecordMetaData meta = new RecordMetaDataLine(this.lineIndex - 1, uri, LineRecordReader.class); //-1 as line number has been incremented already...
        return new org.datavec.api.records.impl.Record(next, meta);
    }
    
    

    @Override
    public Record loadFromMetaData(RecordMetaData recordMetaData) throws IOException {
        return null;
    }

    @Override
    public List<Record> loadFromMetaData(List<RecordMetaData> recordMetaDatas) throws IOException {
       return null;
    }
}