import { Injectable, signal } from '@angular/core';
import { Anuncio, DadosAnuncio } from '../models/anuncio.model';
import { validarAnuncio } from '../models/anuncio.validation';

@Injectable({ providedIn: 'root' })
export class AnunciosDemoService {
  private readonly itens = signal<readonly Anuncio[]>([]);
  readonly anuncios = this.itens.asReadonly();

  criar(dados: DadosAnuncio): Anuncio {
    const erro = validarAnuncio(dados);
    if (erro) throw new Error(erro);

    const anuncio: Anuncio = Object.freeze({
      id: crypto.randomUUID(),
      titulo: dados.titulo.trim(),
      descricao: dados.descricao.trim(),
      categoria: dados.categoria,
      cidade: dados.cidade.trim(),
      modalidade: dados.modalidade,
      preco: dados.modalidade === 'venda' ? dados.preco : null,
      habilidadeDesejada: dados.modalidade === 'troca' ? dados.habilidadeDesejada.trim() : '',
    });
    this.itens.update((itens) => [...itens, anuncio]);
    return anuncio;
  }
}
