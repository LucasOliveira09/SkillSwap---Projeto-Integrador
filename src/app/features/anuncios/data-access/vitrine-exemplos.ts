import { CartaoAnuncio, ItemColecao } from '../ui/vitrine.model';

const imagem = (nome: string) => `images/legado/${nome}`;

// Conteúdo visual do protótipo antigo. Não representa ofertas ou avaliações reais.
export const EXEMPLOS_VITRINE: readonly CartaoAnuncio[] = [
  {
    id: 'exemplo-futebol',
    titulo: 'Aula de Futebol',
    categoria: 'Esportes',
    filtros: ['aulas', 'esportes'],
    imagem: imagem('aula-futebol.jpg'),
    preco: 100,
    unidade: '/mês',
    trocaPor: '',
    detalhes: ['Ter/Qui · 18h–19h', 'Turma reduzida'],
    autor: 'Vini Jr.',
    avatar: imagem('perfil-vini.png'),
    estrelas: 5,
    quando: 'há 1 hora',
    cidade: 'Madrid / ES',
    exemplo: true,
  },
  {
    id: 'exemplo-jiu',
    titulo: 'Aula de Jiu-jitsu',
    categoria: 'Esportes',
    filtros: ['aulas', 'esportes'],
    imagem: imagem('aula-jiu-jitsu.jpg'),
    preco: 80,
    unidade: '/VIP',
    trocaPor: '',
    detalhes: ['Seg/Sex · 14h–16h', 'Aula individual'],
    autor: 'Charles Oliveira',
    avatar: imagem('perfil-charles.jpg'),
    estrelas: 5,
    quando: 'há 4 min',
    cidade: 'Guarulhos / SP',
    exemplo: true,
  },
  {
    id: 'exemplo-faxina',
    titulo: 'Faxina Completa',
    categoria: 'Limpeza',
    filtros: ['manutencao', 'limpeza'],
    imagem: imagem('faxina.jpg'),
    preco: 210,
    unidade: '/dia',
    trocaPor: '',
    detalhes: ['Limpeza de janelas', 'Limpeza de carpetes'],
    autor: 'Lucas Leite',
    avatar: imagem('avatar-lucas.jpg'),
    estrelas: 3,
    quando: 'há 4 horas',
    cidade: 'Fartura / PR',
    exemplo: true,
  },
  {
    id: 'exemplo-capinador',
    titulo: 'Capinador Terreno',
    categoria: 'Serviço',
    filtros: ['manutencao', 'jardinagem'],
    imagem: imagem('capinador.gif'),
    preco: 100,
    unidade: '/dia',
    trocaPor: '',
    detalhes: ['Limpeza de terrenos', 'Manutenção de jardins'],
    autor: 'Emerson Vinicius',
    avatar: imagem('avatar-emerson.jpg'),
    estrelas: 1,
    quando: 'há 8 horas',
    cidade: 'Ribeirão do Sul / SP',
    exemplo: true,
  },
  {
    id: 'exemplo-carona',
    titulo: 'Carona Unifio',
    categoria: 'Corrida',
    filtros: ['transporte'],
    imagem: imagem('carona.webp'),
    preco: 2,
    unidade: '/km',
    trocaPor: '',
    detalhes: ['Seg/Qui · 19h', 'Máximo 3 pessoas'],
    autor: 'Matheus Souza',
    avatar: imagem('avatar-matheus.jpg'),
    estrelas: 4,
    quando: 'há 3 horas',
    cidade: 'Ourinhos / SP',
    exemplo: true,
  },
];

export const CATEGORIAS_VISUAIS: readonly ItemColecao[] = [
  {
    titulo: 'Eletrônicos',
    imagem: imagem('categoria-eletronicos.png'),
    parametros: { categoria: 'eletronicos' },
  },
  {
    titulo: 'Serviços',
    imagem: imagem('categoria-servicos.png'),
    parametros: { categoria: 'manutencao' },
  },
  { titulo: 'Móveis', imagem: imagem('categoria-moveis.png'), parametros: { categoria: 'moveis' } },
  {
    titulo: 'Eletrodomésticos',
    imagem: imagem('categoria-eletrodomesticos.webp'),
    parametros: { categoria: 'eletrodomesticos' },
  },
  { titulo: 'Inglês', imagem: imagem('categoria-ingles.png'), parametros: { busca: 'inglês' } },
  {
    titulo: 'Esportes Paralímpicos',
    imagem: imagem('categoria-paralimpicos.png'),
    parametros: { busca: 'paralímpicos' },
  },
  {
    titulo: 'Perfumes',
    imagem: imagem('categoria-perfumes.png'),
    parametros: { categoria: 'perfumes' },
  },
  {
    titulo: 'Relógios',
    imagem: imagem('categoria-relogios.png'),
    parametros: { categoria: 'relogios' },
  },
  {
    titulo: 'Serviços Braçais',
    imagem: imagem('categoria-bracais.png'),
    parametros: { categoria: 'manutencao' },
  },
];

export const DESTAQUES_VISUAIS: readonly ItemColecao[] = [
  { titulo: 'iPhone', imagem: imagem('destaque-iphone.png'), parametros: { busca: 'iPhone' } },
  { titulo: 'Samsung', imagem: imagem('destaque-samsung.png'), parametros: { busca: 'Samsung' } },
  {
    titulo: 'Eletricista',
    imagem: imagem('destaque-eletricista.png'),
    parametros: { busca: 'eletricista' },
  },
  { titulo: 'Perfumes', imagem: imagem('destaque-perfume.png'), parametros: { busca: 'perfume' } },
  { titulo: 'Relógios', imagem: imagem('destaque-relogio.png'), parametros: { busca: 'relógio' } },
  {
    titulo: 'PlayStation',
    imagem: imagem('destaque-ps5.png'),
    parametros: { busca: 'PlayStation' },
  },
];

export const SERVICOS_VISUAIS: readonly ItemColecao[] = [
  {
    titulo: 'Eletricista',
    imagem: imagem('servico-eletricista.webp'),
    parametros: { busca: 'eletricista' },
  },
  {
    titulo: 'Professor',
    imagem: imagem('servico-professor.png'),
    parametros: { categoria: 'aulas' },
  },
  {
    titulo: 'Encanador',
    imagem: imagem('servico-encanador.png'),
    parametros: { busca: 'encanador' },
  },
  { titulo: 'Capinador', imagem: imagem('capinador.gif'), parametros: { busca: 'capinador' } },
  {
    titulo: 'Pintor de Obras',
    imagem: imagem('servico-pintor.jpg'),
    parametros: { busca: 'pintor' },
  },
  {
    titulo: 'Agricultor',
    imagem: imagem('servico-agricultor.jpg'),
    parametros: { busca: 'agricultor' },
  },
];
