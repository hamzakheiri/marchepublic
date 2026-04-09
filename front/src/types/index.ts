export interface ConsultationDocument {
  id: number;
  type: string | null;
  label: string | null;
  url: string | null;
}

export interface Lot {
  id: number;
  lotNumber: number | null;
  title: string | null;
  category: string | null;
  description: string | null;
  estimation: string | null;
  cautionProvisoire: string | null;
  qualifications: string | null;
  agrements: string | null;
  visitesLieux: string | null;
  variante: string | null;
  considerationsEnv: string | null;
  reserveTpePme: string | null;
}

export interface Consultation {
  id: number;
  refConsultation: string;
  orgAcronyme: string | null;
  procedureType: string | null;
  procedureFullName: string | null;
  category: string | null;
  publishedDate: string | null;
  reference: string | null;
  object: string | null;
  buyer: string | null;
  location: string | null;
  deadline: string | null;
  detailUrl: string | null;
  lotsPopupUrl: string | null;
}
