import { CATEGORIAS_ANUNCIO, DadosAnuncio } from './anuncio.model';

export function validarAnuncio(dados: DadosAnuncio): string | null {
  const campos = [
    { valor: dados.titulo, nome: 'Título', limite: 100 },
    { valor: dados.descricao, nome: 'Descrição', limite: 1500 },
    { valor: dados.cidade, nome: 'Cidade', limite: 80 },
  ];

  for (const campo of campos) {
    if (!campo.valor.trim()) return `${campo.nome}: preencha este campo.`;
    if (campo.valor.length > campo.limite) {
      return `${campo.nome}: use até ${campo.limite} caracteres.`;
    }
  }

  if (!CATEGORIAS_ANUNCIO.some((categoria) => categoria.valor === dados.categoria)) {
    return 'Selecione uma categoria válida.';
  }
  if (dados.modalidade === 'venda') {
    if (dados.preco === null || !Number.isFinite(dados.preco) || dados.preco <= 0) {
      return 'Informe um preço de venda maior que zero.';
    }
  } else if (dados.modalidade === 'troca') {
    if (!dados.habilidadeDesejada.trim() || dados.habilidadeDesejada.length > 120) {
      return 'Informe a habilidade desejada em até 120 caracteres.';
    }
  } else {
    return 'Selecione uma modalidade válida.';
  }

  return null;
}
