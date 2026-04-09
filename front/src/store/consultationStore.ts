import { create } from 'zustand';
import { Consultation, Lot, ConsultationDocument } from '../types';

interface ConsultationState {
  consultations: Consultation[];
  selectedConsultation: Consultation | null;
  lots: Lot[];
  documents: ConsultationDocument[];

  setConsultations: (consultations: Consultation[]) => void;
  setSelectedConsultation: (consultation: Consultation | null) => void;
  setLots: (lots: Lot[]) => void;
  setDocuments: (documents: ConsultationDocument[]) => void;
}

export const useConsultationStore = create<ConsultationState>((set) => ({
  consultations: [],
  selectedConsultation: null,
  lots: [],
  documents: [],

  setConsultations: (consultations) => set({ consultations }),
  setSelectedConsultation: (consultation) => set({ selectedConsultation: consultation }),
  setLots: (lots) => set({ lots }),
  setDocuments: (documents) => set({ documents }),
}));
