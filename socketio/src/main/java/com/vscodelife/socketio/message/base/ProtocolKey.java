package com.vscodelife.socketio.message.base;

public class ProtocolKey {
        private final int mainNo;
        private final int subNo;

        public ProtocolKey(int mainNo, int subNo) {
            this.mainNo = mainNo;
            this.subNo = subNo;
        }

        public int getMainNo() {
            return mainNo;
        }

        public int getSubNo() {
            return subNo;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ProtocolKey that = (ProtocolKey) obj;
            return mainNo == that.mainNo && subNo == that.subNo;
        }

        @Override
        public int hashCode() {
            return mainNo * 1000 + subNo;
        }

        @Override
        public String toString() {
            return String.format("ProtocolKey[%d,%d]", mainNo, subNo);
        }
}
