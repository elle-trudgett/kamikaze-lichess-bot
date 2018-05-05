/*
 * Copyright 2017 Ben-Hur Carlos Vieira Langoni Junior
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package chesslib.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;

public class LargeFile implements Iterable<String> {
    private BufferedReader reader;

    public LargeFile(String filePath) throws Exception {
        reader = new BufferedReader(new FileReader(filePath));
    }

    public void close() {
        try {
            reader.close();
        } catch (Exception ex) {
        }
    }

    public Iterator<String> iterator() {
        return new FileIterator();
    }

    private class FileIterator implements Iterator<String> {
        private String _currentLine;

        public boolean hasNext() {
            try {
                _currentLine = reader.readLine();
            } catch (Exception ex) {
                _currentLine = null;
                ex.printStackTrace();
            }

            return _currentLine != null;
        }

        public String next() {
            return _currentLine;
        }

        public void remove() {
        }
    }
}
