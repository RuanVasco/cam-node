export const MESSAGES = {
  appTitle: 'Campainha',
  connection: {
    connecting: 'Conectando',
    live: 'Ao vivo',
    reconnecting: 'Reconectando',
  },
  history: {
    empty: 'Nenhum toque registrado ainda.',
    loadMore: 'Carregar mais',
    today: 'Hoje',
    yesterday: 'Ontem',
    photoAlt: 'Foto do toque',
    openPhoto: (time: string) => `Ampliar foto do toque das ${time}`,
  },
  photoDialog: {
    close: 'Fechar',
  },
  removal: {
    remove: 'Remover',
    removeRing: (time: string) => `Remover toque das ${time}`,
    select: 'Selecionar',
    cancelSelection: 'Cancelar seleção',
    selectRing: (time: string) => `Selecionar toque das ${time}`,
    removeSelected: (count: number) => `Remover selecionados (${count})`,
    confirmOne: 'Remover este toque? A foto será apagada definitivamente.',
    confirmMany: (count: number) =>
      `Remover ${count} toques? As fotos serão apagadas definitivamente.`,
    cancel: 'Cancelar',
    confirm: 'Remover',
    removedOne: 'Toque removido.',
    removedMany: (count: number) => `${count} toques removidos.`,
  },
  adminKey: {
    title: 'Senha de administrador',
    label: 'Senha',
    cancel: 'Cancelar',
    confirm: 'Confirmar',
    wrong: 'Senha incorreta.',
  },
  errors: {
    generic: 'Não foi possível carregar os dados. Tente novamente.',
    removalFailed: 'Não foi possível remover. Tente novamente.',
    tooManyAttempts: 'Muitas tentativas. Tente de novo em alguns minutos.',
    dismiss: 'OK',
  },
};
